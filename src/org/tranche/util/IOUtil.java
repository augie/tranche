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
package org.tranche.util;

import org.tranche.security.SecurityUtil;
import org.tranche.configuration.ConfigurationUtil;
import org.tranche.configuration.Configuration;
import org.tranche.hash.BigHash;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ServerSocketFactory;
import org.tranche.TrancheServer;
import org.tranche.security.Signature;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.flatfile.DataBlockUtil;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.project.ProjectFile;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.server.PropagationReturnWrapper;
import org.tranche.server.Server;
import org.tranche.streams.DeleteFileOnExitFileInputStream;

/**
 * <p>Various helper methods for serializing data for storage and transfer as well as reading data from disk or network.</p>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class IOUtil {

    private static final String EOL = "\n";
    public static final byte BYTE_FALSE = (byte) 0;
    public static final byte BYTE_TRUE = (byte) 1;

    /**
     * <p>Helper method to get all the bytes from a file using buffered IO..</p>
     * @param f File to read bytes from.
     * @throws java.io.IOException If the file can't be found or if the file isn't readable.
     * @return A byte[] representing the contents of the file.
     */
    public static byte[] getBytes(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        byte[] b = getBytes(fis);
        fis.close();
        return b;
    }

    /**
     * <p>Set the bytes to a file. Overwrites existing content (no append).</p>
     * @param bytes
     * @param f
     * @throws java.io.IOException
     */
    public static void setBytes(byte[] bytes, File f) throws IOException {
        setBytes(bytes, f, false);
    }

    /**
     * <p>Set the bytes to a file.</p>
     * @param bytes
     * @param f
     * @param append True if append, false otherwise
     * @throws java.io.IOException
     */
    public static void setBytes(byte[] bytes, File f, boolean append) throws IOException {
        // make sure that the file exists
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        // write out the bytes
        FileOutputStream fos = new FileOutputStream(f, append);
        fos.write(bytes);
        fos.flush();
        fos.close();
    }

    /**
     * <p>Read in all the bytes from an InputStream.</p>
     * <p>Note that these bytes are buffered in memory.</p>
     * @param in
     * @return
     * @throws java.io.IOException
     */
    public static byte[] getBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1000];
        for (int bytesRead = in.read(buf); bytesRead > -1; bytesRead = in.read(buf)) {
            baos.write(buf, 0, bytesRead);
        }
        baos.flush();
        // return the buffered bytes
        return baos.toByteArray();
    }

    /**
     * <p>Read all the bytes from an InputStream and write them to an OutputStream.</p>
     * <p>Buffers maximum of 10KB at a time.</p>
     * @param in
     * @param out
     * @throws java.io.IOException
     */
    public static void getBytes(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[10 * 1024];
        for (int bytesRead = in.read(buf); bytesRead > -1; bytesRead = in.read(buf)) {
            out.write(buf, 0, bytesRead);
        }
        // flush
        out.flush();
    }

    /**
     * <p>Builds a URL from parameters. Format: tranche://host:port or ssl+tranche://host:port</p>
     * @param host
     * @param port
     * @param secure
     * @return
     */
    public static String createURL(String host, int port, boolean secure) {
        // return either SSL or normal
        if (secure) {
            return "ssl+tranche://" + host + ":" + port;
        } else {
            return "tranche://" + host + ":" + port;
        }
    }

    /**
     * <p>Create a Tranche url from any TrancheServer. If fails, returns null.</p>
     * @return
     */
    public static String createURL(TrancheServer dfs) {
        // try remote
        if (dfs instanceof RemoteTrancheServer) {
            RemoteTrancheServer rdfs = (RemoteTrancheServer) dfs;
            // return either SSL or normal
            if (rdfs.isSecure()) {
                return "ssl+tranche://" + rdfs.getHost() + ":" + rdfs.getPort();
            } else {
                return "tranche://" + rdfs.getHost() + ":" + rdfs.getPort();
            }
        }

        // try a flat file based file system
        if (dfs instanceof FlatFileTrancheServer) {
            FlatFileTrancheServer ffdfs = (FlatFileTrancheServer) dfs;
            String url = "file://" + ffdfs.getHomeDirectory().getAbsolutePath();
            // replace C: with /
            url = url.replaceAll("C:", "");
            url = url.replaceAll("\\\\", "/");
            return url;
        }

        return null;
    }

    /**
     * <p>A helper method to safely close off any resources used by ProjectFile. (Might use a DiskBackedProjectFilePartSet, which uses a temporary file.)
     * @param pf
     */
    public static final void safeClose(ProjectFile pf) {
        if (pf != null) {
            try {
                pf.close();
            } catch (Exception e) {
            }
        }
    }
    
    /**
     * <p>Helper method to safely close a DataBlockUtil.</p>
     * @param dbu
     */
    public static final void safeClose(DataBlockUtil dbu) {
        try {
            dbu.close();
        } catch (Exception e) {}
    }

    /**
     * <p>A helper method to safely close the given OutputStream object via invoking flush() followed by close(). Any exceptions thorwn are discarded silently, including NullPointerExceptions that may be thrown if the reference is null.</p>
     * @param out OuputStream to safely close.
     */
    public static final void safeClose(OutputStream out) {
        if (out != null) {
            try {
                out.flush();
            } catch (Exception e) {
            }
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>A helper method to safely close the given FileWriter object via invoking flush() followed by close(). Any exceptions thorwn are discarded silently, including NullPointerExceptions that may be thrown if the reference is null.</p>
     * @param out OuputStream to safely close.
     */
    public static final void safeClose(Writer out) {
        if (out != null) {
            try {
                out.flush();
            } catch (Exception e) {
            }
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Safely closes an InputStream by invoking the close() method. Any errors thrown are silently discard, and no NullPointerException is thrown if the InputStream reference passed is null.</p>
     * @param in InpuStream to safely close.
     */
    public static final void safeClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Safely close off resources used by RandomAccessFile.</p>
     * @param ras
     */
    public static final void safeClose(RandomAccessFile ras) {
        if (ras != null) {
            try {
                ras.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Safely close off resources used by any Reader.</p>
     * @param in
     */
    public static void safeClose(Reader in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Helper method to make sure that the DFS closes and that no exceptions are thrown, including NullPointerExceptions.</p>
     * @param dfs DistributedFileSystem instance to safely close.
     */
    public static final void safeClose(TrancheServer dfs) {
        if (dfs != null) {
            try {
                dfs.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>Helper method to make sure that the DFS closes and that no exceptions are thrown, including NullPointerExceptions.</p>
     * @param server passed in Server
     */
    public static final void safeClose(Server server) {
        if (server != null) {
            try {
                server.setRun(false);
            } catch (Exception e) {
            }
            try {
                server.join();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 
     * @param socket
     */
    public static final void safeClose(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 
     * @param serverSocket
     */
    public static final void safeClose(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * <p>A helper method that trys really hard to make sure a file is deleted. If the file can't be deleted, it is reduced to a size of 1. This method is particuarlly helpful for the Knoppix LiveCDs and making sure temporary files are deleted. If they aren't they'll quickly saturate the ramdisk and cause "Out of disk space" errors.</p>
     * @param file The file to delete.
     */
    public static void safeDelete(File file) {
        if (file == null) {
            return;
        }
        if (!file.delete() && file.exists() && !file.isDirectory()) {
            FileOutputStream temp = null;
            try {
                temp = new FileOutputStream(file);
                temp.write('0');
            } catch (Exception e) {
            } finally {
                IOUtil.safeClose(temp);
            }
        }
    }

    /**
     * <p>Recursively deletes all the files in the directory. If fails, throws RuntimeException.</p>
     * @param dir
     * @return The directory passed as a parameter.
     */
    public static File recursiveDelete(File dir) {
        if (dir != null && dir.exists()) {
            if (dir.isDirectory()) {
                for (String fname : dir.list()) {
                    File ff = new File(dir, fname);
                    recursiveDelete(ff);
                }
            }
            if (!dir.delete()) {
                if (dir.exists()) {
                    throw new RuntimeException("Can't delete " + dir);
                }
            }
        }

        return dir;
    }

    /**
     * <p>Recursively deletes all the files in the directory. If the deletion can't completely finish, show a warning.</p>
     * @param dir
     * @return The directory passed as a parameter.
     */
    public static final File recursiveDeleteWithWarning(File dir) {
        try {
            return recursiveDelete(dir);
        } catch (Throwable e) {
            try {
                System.out.println("Warning! Can't fully delete " + dir.getCanonicalPath());
            } catch (Exception ex) {
                // noop
            }
        }
        return dir;
    }

    /**
     * <p>Helper method to transfer the given size of bytes from the InputStream to the OutputStream. Reasonably buffered.</p>
     * @param out
     * @param is
     * @param size Maximum bytes to read.
     * @throws java.io.IOException
     */
    public static final void getBytes(final OutputStream out, final InputStream is, final long size) throws IOException {
        // write out the data
        long index = 0;
        byte[] buf = new byte[10000];
        int maxToRead = buf.length;
        if (size - index < maxToRead) {
            maxToRead = (int) (size - index);
        }
        for (int bytesRead = is.read(buf, 0, maxToRead); bytesRead != -1 && index < size; bytesRead = is.read(buf, 0, maxToRead)) {
            index += bytesRead;
            out.write(buf, 0, bytesRead);
            if (size - index < maxToRead) {
                maxToRead = (int) (size - index);
            }
        }
    }

    /**
     * <p>Helper method to transfer the given size of bytes from the InputStream to the OutputStream. Reasonably buffered.</p>
     * @param raf
     * @param buf
     * @throws java.io.IOException
     */
    public static final void getBytes(final RandomAccessFile raf, final byte[] buf) throws IOException {
        // write out the data
        int index = 0;
        int maxToRead = buf.length;
        for (int bytesRead = raf.read(buf, index, maxToRead); bytesRead != -1 && index < buf.length; bytesRead = raf.read(buf, index, maxToRead)) {
            index += bytesRead;
            if (buf.length - index < maxToRead) {
                maxToRead = buf.length - index;
            }
        }
    }

    /**
     * <p>Helper method to transfer the given size of bytes from the InputStream to the OutputStream. Reasonably buffered.</p>
     * @param raf
     * @param buf
     * @return
     * @throws java.io.IOException
     */
    public static final int getBytes(final InputStream raf, final byte[] buf) throws IOException {
        // write out the data
        int index = 0;
        int maxToRead = buf.length;
        for (int bytesRead = raf.read(buf, index, maxToRead); bytesRead != -1 && index < buf.length; bytesRead = raf.read(buf, index, maxToRead)) {
            index += bytesRead;
            if (buf.length - index < maxToRead) {
                maxToRead = buf.length - index;
            }
        }
        // return exactly how many bytes where read
        return index;
    }

    /**
     * <p>Utility method to fill buffer with bytes from input stream.</p>
     * <p>Throws an IOException if reaches end of stream before reading all bytes.</p>
     * @param buffer
     * @param bais
     * @throws java.io.IOException 
     */
    public static final void getBytesFully(byte[] buffer, InputStream bais) throws IOException {
        long size = 0;
        while (size < buffer.length) {
            long read = bais.read(buffer);

            if (read == -1) {
                throw new IOException("Unexpected end of input stream, -1 read. (Total bytes read: " + size + " out of expected " + buffer.length + ")");
            }

            size += read;
        }
    }

    /**
     *
     * @param ts
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    public static final boolean hasData(TrancheServer ts, BigHash hash) throws Exception {
        BigHash[] hashes = new BigHash[1];
        hashes[0] = hash;
        return ts.hasData(hashes)[0];
    }

    /**
     *
     * @param ts
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    public static final boolean hasMetaData(TrancheServer ts, BigHash hash) throws Exception {
        BigHash[] hashes = new BigHash[1];
        hashes[0] = hash;
        return ts.hasMetaData(hashes)[0];
    }

    /**
     * 
     * @param ts
     * @param cert
     * @param key
     * @param merge
     * @param hash
     * @param bytesToUpload
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper setMetaData(TrancheServer ts, X509Certificate cert, PrivateKey key, boolean merge, BigHash hash, byte[] bytesToUpload) throws Exception {
        String[] hosts = new String[1];
        hosts[0] = createHostInternal(ts);
        return setMetaData(ts, cert, key, merge, hash, bytesToUpload, hosts);
    }

    /**
     * <p>Set the meta data bytes associated with the hash to selected servers through a particular TrancheServer. The chunk will be signed.</p>
     * @param dfs
     * @param cert
     * @param key
     * @param merge
     * @param hash
     * @param bytesToUpload
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper setMetaData(TrancheServer dfs, X509Certificate cert, PrivateKey key, boolean merge, BigHash hash, byte[] bytesToUpload, String[] hosts) throws Exception {
        if (bytesToUpload == null) {
            throw new Exception("Can't have a null bytes to upload!");
        }

        // sign nonce + hash + data
        byte[] bytes = new byte[BigHash.HASH_LENGTH + bytesToUpload.length];
        byte[] hashBytes = hash.toByteArray();
        System.arraycopy(hashBytes, 0, bytes, 0, hashBytes.length);
        System.arraycopy(bytesToUpload, 0, bytes, hashBytes.length, bytesToUpload.length);

        // sign the bytes
        String algorithm = SecurityUtil.getSignatureAlgorithm(key);
        byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), key, algorithm);

        // make a proper signature object
        Signature signature = new Signature(sig, algorithm, cert);

        // add the data to the dfs
        return dfs.setMetaData(merge, hash, bytesToUpload, signature, hosts);
    }

    /**
     * 
     * @param ts
     * @param cert
     * @param key
     * @param hash
     * @param bytesToUpload
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper setData(TrancheServer ts, X509Certificate cert, PrivateKey key, BigHash hash, byte[] bytesToUpload) throws Exception {
        String[] hosts = new String[1];
        hosts[0] = createHostInternal(ts);
        return setData(ts, cert, key, hash, bytesToUpload, hosts);
    }

    /**
     * 
     * @param dfs
     * @param cert
     * @param key
     * @param hash
     * @param bytesToUpload
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper setData(TrancheServer dfs, X509Certificate cert, PrivateKey key, BigHash hash, byte[] bytesToUpload, String[] hosts) throws Exception {
        if (bytesToUpload == null) {
            throw new Exception("Can't have a null bytes to upload!");
        }

        // sign nonce + hash + data
        byte[] bytes = new byte[BigHash.HASH_LENGTH + bytesToUpload.length];
        byte[] hashBytes = hash.toByteArray();
        System.arraycopy(hashBytes, 0, bytes, 0, hashBytes.length);
        System.arraycopy(bytesToUpload, 0, bytes, hashBytes.length, bytesToUpload.length);

        // sign the bytes
        String algorithm = SecurityUtil.getSignatureAlgorithm(key);
        byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), key, algorithm);

        // make a proper signature object
        Signature signature = new Signature(sig, algorithm, cert);

        // add the data to the dfs
        return dfs.setData(hash, bytesToUpload, signature, hosts);
    }

    /**
     * <p>Method to return appropriate host so that production code and tests work.</p>
     * @param ts
     * @return
     */
    private static String createHostInternal(TrancheServer ts) {
        if (TestUtil.isTesting() && TestUtil.isTestingManualNetworkStatusTable() && ts instanceof RemoteTrancheServer) {
            String url = IOUtil.createURL(ts);
            String host = TestUtil.getOstensibleHostForURL(url);

            // If didn't find host, let's try the host the server returned...
            if (host == null) {
                return ts.getHost();
            } else {
                return host;
            }
        } else {
            return ts.getHost();
        }
    }

    /**
     * 
     * @param ts
     * @return
     * @throws java.lang.Exception
     */
    public static final byte[] getNonce(TrancheServer ts) throws Exception {
        try {
            return getNonces(ts, 1)[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 
     * @param ts
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public static final byte[][] getNonces(TrancheServer ts, String[] hosts) throws Exception {
        PropagationReturnWrapper wrapper = ts.getNonces(hosts, 1);
        if (wrapper.isAnyErrors()) {
            throw wrapper.getErrors().iterator().next().exception;
        }

        byte[][] nonces = new byte[hosts.length][];
        byte[][][] response = (byte[][][]) wrapper.getReturnValueObject();
        for (int i = 0; i < hosts.length; i++) {
            nonces[i] = response[i][0];
        }

        return nonces;
    }

    /**
     * 
     * @param ts
     * @param count
     * @return
     * @throws java.lang.Exception
     */
    public static final byte[][] getNonces(TrancheServer ts, int count) throws Exception {

        // set up
        String[] hosts = new String[1];
        hosts[0] = createHostInternal(ts);

        // execute
        PropagationReturnWrapper wrapper = ts.getNonces(hosts, count);
        if (wrapper.isAnyErrors()) {
            throw wrapper.getErrors().iterator().next().exception;
        }

        // return the first host
        return ((byte[][][]) wrapper.getReturnValueObject())[0];
    }

    /**
     * <p>Adding meta-data and data bytes is near-identical. This  method abstracts that work.</p>
     * @param dfs
     * @param cert
     * @param key
     * @return
     * @throws java.lang.Exception
     */
    public static final Configuration getConfiguration(TrancheServer dfs, X509Certificate cert, PrivateKey key) throws Exception {
        // get a nonce from the server
        byte[] nonce = getNonce(dfs);

        // sign the bytes
        String algorithm = SecurityUtil.getSignatureAlgorithm(key);
        byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(nonce), key, algorithm);

        // make a proper signature object
        Signature signature = new Signature(sig, algorithm, cert);

        // request the configuration
        return dfs.getConfiguration(signature, nonce);
    }

    /**
     * <p>Remotely request a shutdown. Uses certificate and private key to authenticate user remotely.</p>
     * <p>Requires administrative privileges on remote server.</p>
     * @param ts
     * @param cert
     * @param key
     * @throws java.lang.Exception
     */
    public static final void requestShutdown(TrancheServer ts, X509Certificate cert, PrivateKey key) throws Exception {
        byte[] nonce = getNonce(ts);

        // sign the bytes
        String algorithm = SecurityUtil.getSignatureAlgorithm(key);
        byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(nonce), key, algorithm);

        // make a proper signature object
        Signature signature = new Signature(sig, algorithm, cert);

        ts.requestShutdown(signature, nonce);
    }

    /**
     * <p>Adding meta-data and data bytes is near-identical. This  method abstracts that work.</p>
     * @param ts
     * @param config
     * @param cert
     * @param key
     * @throws java.lang.Exception
     */
    public static final void setConfiguration(TrancheServer ts, Configuration config, X509Certificate cert, PrivateKey key) throws Exception {
        // get a nonce from the server
        byte[] nonce = getNonce(ts);

        // save to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ConfigurationUtil.write(config, baos);
        byte[] configBytes = baos.toByteArray();

        // make the bytes
        byte[] bytes = new byte[configBytes.length + nonce.length];
        System.arraycopy(configBytes, 0, bytes, 0, configBytes.length);
        System.arraycopy(nonce, 0, bytes, configBytes.length, nonce.length);

        // sign the bytes
        String algorithm = SecurityUtil.getSignatureAlgorithm(key);
        byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), key, algorithm);

        // make a proper signature object
        Signature signature = new Signature(sig, algorithm, cert);

        // request the configuration
        ts.setConfiguration(configBytes, signature, nonce);
    }

    /**
     * 
     * @param ts
     * @param hash
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper getData(TrancheServer ts, BigHash hash, boolean propagateRequest) throws Exception {
        BigHash[] hashes = new BigHash[1];
        hashes[0] = hash;
        return ts.getData(hashes, propagateRequest);
    }

    /**
     * 
     * @param wrapper
     * @return
     */
    public static final byte[] get1DBytes(PropagationReturnWrapper wrapper) {
        try {
            if (!wrapper.isVoid()) {
                return ((byte[][]) wrapper.getReturnValueObject())[0];
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     *
     * @param wrapper
     * @return
     */
    public static final byte[][] get2DBytes(PropagationReturnWrapper wrapper) {
        try {
            if (!wrapper.isVoid()) {
                return (byte[][]) wrapper.getReturnValueObject();
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 
     * @param ts
     * @param hash
     * @param propagateRequest
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper getMetaData(TrancheServer ts, BigHash hash, boolean propagateRequest) throws Exception {
        BigHash[] hashes = new BigHash[1];
        hashes[0] = hash;
        return ts.getMetaData(hashes, propagateRequest);
    }

    /**
     * 
     * @param ts
     * @param cert
     * @param key
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper deleteData(TrancheServer ts, X509Certificate cert, PrivateKey key, BigHash hash) throws Exception {
        String[] hosts = new String[1];
        hosts[0] = createHostInternal(ts);

        return deleteData(ts, cert, key, hash, hosts);
    }

    /**
     * <p>Delete a particular chunk from a remote server.</p>
     * <p>Requires appropriate privileges. Uses certificate and key to remotely authenticate.</p>
     * @param dfs
     * @param cert
     * @param key
     * @param hash
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper deleteData(TrancheServer dfs, X509Certificate cert, PrivateKey key, BigHash hash, String[] hosts) throws Exception {

        StringBuffer hostsStr = new StringBuffer();
        for (String host : hosts) {
            hostsStr.append(host + " ");
        }

        byte[][] nonces = getNonces(dfs, hosts);

        // Verify assertions
        if (nonces.length != hosts.length) {
            throw new AssertionFailedException("nonces.length<" + nonces.length + "> != hosts.length<" + hosts.length + ">");
        }

        Signature[] signatures = new Signature[hosts.length];

        // Create the signatures (sign the nonce + hash)
        for (int i = 0; i < hosts.length; i++) {
            byte[] hashBytes = hash.toByteArray();
            byte[] bytes = new byte[nonces[i].length + hashBytes.length];
            System.arraycopy(nonces[i], 0, bytes, 0, nonces[i].length);
            System.arraycopy(hashBytes, 0, bytes, nonces[i].length, hashBytes.length);

            String algorithm = SecurityUtil.getSignatureAlgorithm(key);
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), key, algorithm);

            // make a proper signature object
            signatures[i] = new Signature(sig, algorithm, cert);
        }

        // add the data to the dfs
        return dfs.deleteData(hash, signatures, nonces, hosts);
    }

    /**
     * <p>Delete a particular meta data chunk from a remote server.</p>
     * <p>Requires appropriate privileges. Uses certificate and key to remotely authenticate.</p>
     * @param dfs
     * @param cert
     * @param key
     * @param hash
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper deleteMetaData(TrancheServer dfs, X509Certificate cert, PrivateKey key, BigHash hash) throws Exception {
        return deleteMetaData(dfs, cert, key, null, null, null, hash);
    }

    /**
     * <p>Delete a particular meta data chunk from selected servers through a remote server.</p>
     * <p>Requires appropriate privileges. Uses certificate and key to remotely authenticate.</p>
     * @param dfs
     * @param cert
     * @param key
     * @param hash
     * @param hosts
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper deleteMetaData(TrancheServer dfs, X509Certificate cert, PrivateKey key, BigHash hash, String[] hosts) throws Exception {
        return deleteMetaData(dfs, cert, key, null, null, null, hash, hosts);
    }

    /**
     * 
     * @param ts
     * @param cert
     * @param key
     * @param uploaderName
     * @param uploadTimestamp
     * @param relativePathInDataSet
     * @param hash
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper deleteMetaData(TrancheServer ts, X509Certificate cert, PrivateKey key, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, BigHash hash) throws Exception {
        String[] hosts = new String[1];
        hosts[0] = createHostInternal(ts);
        return deleteMetaData(ts, cert, key, uploaderName, uploadTimestamp, relativePathInDataSet, hash, hosts);
    }

    /**
     * 
     * @param dfs
     * @param cert
     * @param key
     * @param uploaderName
     * @param uploadTimestamp
     * @param relativePathInDataSet
     * @param hash
     * @param hosts
     * @return
     * @throws java.lang.Exception
     */
    public static final PropagationReturnWrapper deleteMetaData(TrancheServer dfs, X509Certificate cert, PrivateKey key, String uploaderName, Long uploadTimestamp, String relativePathInDataSet, BigHash hash, String[] hosts) throws Exception {
        // get nonces from the servers
        byte[][] nonces = getNonces(dfs, hosts);

        // Verify assertions
        if (nonces.length != hosts.length) {
            throw new AssertionFailedException("nonces.length<" + nonces.length + "> != hosts.length<" + hosts.length + ">");
        }

        Signature[] signatures = new Signature[hosts.length];

        // Create the signatures (sign the nonce + hash)
        for (int i = 0; i < hosts.length; i++) {
            byte[] hashBytes = hash.toByteArray();
            byte[] bytes = new byte[nonces[i].length + hashBytes.length];
            System.arraycopy(nonces[i], 0, bytes, 0, nonces[i].length);
            System.arraycopy(hashBytes, 0, bytes, nonces[i].length, hashBytes.length);

            String algorithm = SecurityUtil.getSignatureAlgorithm(key);
            byte[] sig = SecurityUtil.sign(new ByteArrayInputStream(bytes), key, algorithm);

            // make a proper signature object
            signatures[i] = new Signature(sig, algorithm, cert);
        }

        // execute
        return dfs.deleteMetaData(hash, uploaderName, uploadTimestamp, relativePathInDataSet, signatures, nonces, hosts);
    }

    /**
     * <p>Write a line to a Writer. Safely escapes backslashes and newlines.</p>
     * @param s
     * @param w
     * @throws java.io.IOException
     */
    public static final void writeLine(String s, Writer w) throws IOException {
        // filter the string
        s = s.replace("\\", "\\\\");
        s = s.replace("\n", "\\n");
        w.write(s);
        w.write(EOL);
    }

    /**
     * <p>Read a line from a BufferedReader. Unescapes backslashes and newlines.</p>
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static final String readLine(final BufferedReader is) throws IOException {
        String s = is.readLine();
        if (s == null) {
            return null;
        }
        s = s.replace("\\n", "\n");
        s = s.replace("\\\\", "\\");
        return s;
    }

    /**
     * <p>Helper method to attempt to rename a file. If a soft link fails (e.g. rename across disks) attempt a slower hard copy.</p>
     * @param toRename
     * @param renameTo
     * @return
     */
    public static final boolean renameFallbackCopy(File toRename, File renameTo) {
        // throw an exception if the first file doesn't exist
        if (!toRename.exists()) {
            throw new RuntimeException("Can't rename non-existant file " + toRename);
        }

        // if the file already exists, try deleting
        if (renameTo.exists() && !renameTo.delete()) {
            throw new RuntimeException("Can't delete " + renameTo + " before moving " + toRename);
        }
        File parent = renameTo.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // files that won't rename might just be on different disks. try copy/delete.
        if (!toRename.renameTo(renameTo)) {
            FileOutputStream fos = null;
            FileInputStream fis = null;
            try {
                fos = new FileOutputStream(renameTo);
                fis = new FileInputStream(toRename);
                IOUtil.getBytes(fis, fos);
                //close down stream
                IOUtil.safeClose(fos);
                IOUtil.safeClose(fis);
                // delete the file
                if (!toRename.delete()) {
                    toRename.deleteOnExit();
                }
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException("Can't copy file.");
            } finally {
                IOUtil.safeClose(fos);
                IOUtil.safeClose(fis);
            }
        }
        return true;
    }

    /**
     * <p>Parses a protocol (e.g., tranche://, http://, etc.) from a URL.</p>
     * @param url
     * @return
     * @throws java.lang.Exception
     */
    public static String parseProtocol(String url) throws Exception {

        String protocol;

        Pattern pattern = Pattern.compile("^.*?://");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            protocol = matcher.group();
        } else {
            throw new Exception("URL missing protocol");
        }

        return protocol;
    }

    /**
     * <p>Parses a port from a URL.</p>
     * @param url
     * @return
     * @throws java.lang.Exception
     */
    public static int parsePort(String url) throws Exception {

        int port;

        Pattern pattern = Pattern.compile("^.*?://.*?:(\\d+)$");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            port = Integer.parseInt(matcher.group(1));
        } else {
            throw new Exception("URL<" + url + "> missing port");
        }

        return port;
    }

    /**
     * 
     * @param url
     * @return
     * @throws java.lang.Exception
     */
    public static boolean parseSecure(String url) throws Exception {
        return url.toLowerCase().startsWith("ssl");
    }

    /**
     * <p>Parse the host from the URL. Generally used for the IP, though handles host names, too.</p>
     * @param urlOrHost
     * @return
     */
    public static String parseHost(String urlOrHost) {

        String host;

        Pattern pattern = Pattern.compile("^.*?://(.*)?:\\d+$");
        Matcher matcher = pattern.matcher(urlOrHost);

        try {
            if (matcher.find()) {
                host = matcher.group(1);
            } else {
                throw new Exception("URL missing protocol");
            }
        } catch (Exception e) {
            host = urlOrHost;
        }

        return host;
    }

    /**
     * <p>Copies any file in a memory-safe manner.</p>
     * @param src 
     * @param dest If any data exists, it will be over-written (clobbered).
     * @throws java.io.IOException
     */
    public static void copyFile(File src, File dest) throws IOException {
        FileInputStream fis = null;
        InputStream in = null;
        FileOutputStream fos = null;
        OutputStream out = null;
        try {
            fis = new FileInputStream(src);
            in = new BufferedInputStream(fis);
            fos = new FileOutputStream(dest);
            out = new BufferedOutputStream(fos);

            byte[] buffer = new byte[256];
            int bytes;
            while ((bytes = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytes);
            }
        } finally {
            IOUtil.safeClose(in);
            IOUtil.safeClose(fis);
            IOUtil.safeClose(out);
            IOUtil.safeClose(fos);
        }
    }

    /**
     * <p>Recursively copies from src to dest. File can reference directories or normal files.</p>
     * @param src
     * @param dest
     * @throws java.io.IOException
     */
    public static void recursiveCopyFiles(File src, File dest) throws IOException {
        recursiveCopyFiles(src, dest, false);
    }

    /**
     * <p>Recursively copies from src to dest. File can reference directories or normal files.</p>
     * @param src
     * @param dest
     * @param shouldContinueIfDestExists True if want to copy even if destination; false will throw an exception if destination exists
     * @throws java.io.IOException
     */
    public static void recursiveCopyFiles(File src, File dest, boolean shouldContinueIfDestExists) throws IOException {
        // Depth-first to avoid memory issues
        List<String> filePathsToCopy = new ArrayList();
        filePathsToCopy.add(src.getAbsolutePath());

        File nextFileToCopy, nextDestFile;

        if (dest.exists() && dest.isFile() && !shouldContinueIfDestExists) {
            throw new IOException("Cannot copy file to file " + dest.getAbsolutePath() + ", destination exists!");
        }

        if (dest.exists() && dest.isDirectory() && dest.list().length != 0 && !shouldContinueIfDestExists) {
            throw new IOException("Cannot copy file to directory " + dest.getAbsolutePath() + ", destination exists!");
        }

        while (!filePathsToCopy.isEmpty()) {
            nextFileToCopy = new File(filePathsToCopy.remove(0));
            nextDestFile = new File(nextFileToCopy.getAbsolutePath().replace(src.getAbsolutePath(), dest.getAbsolutePath()));

            if (!nextFileToCopy.exists()) {
                throw new IOException("Cannot copy " + nextFileToCopy.getAbsolutePath() + ": File does not exist!");
            } else if (nextFileToCopy.isDirectory()) {
                nextDestFile.mkdirs();
                if (!nextDestFile.exists() && !nextDestFile.mkdirs()) {
                    throw new IOException("Failed to make: " + nextDestFile.getAbsolutePath() + " (Writable?=" + nextDestFile.canWrite() + ", Readable?=" + nextDestFile.canRead() + ")");
                }
                for (String p : nextFileToCopy.list()) {
                    filePathsToCopy.add(0, nextFileToCopy.getAbsolutePath() + File.separator + p);
                }
            } else {
                // Make sure parent directory exists for target
                if (nextDestFile.getParent() != null && !nextDestFile.getParentFile().exists()) {
                    nextDestFile.getParentFile().mkdirs();
                    if (!nextDestFile.getParentFile().exists()) {
                        throw new IOException("Failed to make parent directory: " + nextDestFile.getParent());
                    }
                }
                copyFile(nextFileToCopy, nextDestFile);
            }
        }
    }

    /**
     * <p>Returns the first port in a range that is available on local machine. Returns -1 if none available.</p>
     * @param startPort
     * @param endPort
     * @return
     */
    public static int findAvailablePort(int startPort, int endPort) {
        int port = -1;
        ServerSocket ss = null;
        try {
            for (int i = startPort; i <= endPort; i++) {
                try {
                    // Try to connect
                    ss = ServerSocketFactory.getDefault().createServerSocket(i);
                    // If get here, open port
                    port = i;
                    break;
                } catch (IOException e) {
                    // Try next...
                }
            }
        } finally {
            IOUtil.safeClose(ss);
        }
        return port;
    }

    public static final void writeBigHash(BigHash v, OutputStream o) throws IOException {
        if (v == null) {
            writeData(null, o);
        } else {
            writeData(v.toByteArray(), o);
        }
    }

    public static final BigHash readBigHash(InputStream i) throws IOException {
        byte[] bytes = readDataBytes(i);
        if (bytes != null) {
            return BigHash.createFromBytes(bytes);
        }
        return null;
    }

    public static final void writeString(String v, OutputStream o) throws IOException {
        if (v == null) {
            writeData(null, o);
        } else {
            writeData(v.getBytes(), o);
        }
    }

    public static final String readString(InputStream i) throws IOException {
        byte[] bytes = readDataBytes(i);
        if (bytes != null) {
            return new String(bytes);
        }
        return null;
    }

    public static final void writeBoolean(boolean v, OutputStream o) throws IOException {
        if (v) {
            writeByte(BYTE_TRUE, o);
        } else {
            writeByte(BYTE_FALSE, o);
        }
    }

    public static final boolean readBoolean(InputStream i) throws IOException {
        if (readByte(i) == BYTE_TRUE) {
            return true;
        } else {
            return false;
        }
    }

    public static final void writeLong(long v, OutputStream o) throws IOException {
        byte[] bytes = new byte[8];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.putLong(v);
        writeBytes(bytes, o);
    }

    public static final long readLong(InputStream i) throws IOException {
        return ByteBuffer.wrap(readBytes(8, i)).getLong();
    }

    public static final void writeInt(int v, OutputStream o) throws IOException {
        byte[] bytes = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.putInt(v);
        writeBytes(bytes, o);
    }

    public static final int readInt(InputStream i) throws IOException {
        return ByteBuffer.wrap(readBytes(4, i)).getInt();
    }

    public static final void writeData(byte[] v, OutputStream o) throws IOException {
        if (v == null) {
            writeInt(-1, o);
        } else {
            writeInt(v.length, o);
            writeBytes(v, o);
        }
    }

    public static final byte[] readDataBytes(InputStream i) throws IOException {
        InputStream buffer = null;
        try {
            buffer = readData(i);
            if (buffer == null) {
                return null;
            } else {
                byte[] bytes = new byte[buffer.available()];
                buffer.read(bytes);
                return bytes;
            }
        } finally {
            safeClose(buffer);
        }
    }

    public static final InputStream readData(InputStream i) throws IOException {
        int byteCount = IOUtil.readInt(i);
        if (byteCount == -1) {
            return null;
        } else if (byteCount <= DataBlockUtil.ONE_MB) {
            return new ByteArrayInputStream(readBytes(byteCount, i));
        }
        // make a new file
        File temp = TempFileUtil.createTemporaryFile();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(temp);
            getBytes(i, fos);
        } finally {
            IOUtil.safeClose(fos);
        }
        return new DeleteFileOnExitFileInputStream(temp);
    }

    public static final void writeBytes(byte[] v, OutputStream o) throws IOException {
        o.write(v);
        o.flush();
    }

    public static final byte[] readBytes(int byteCount, InputStream i) throws IOException {
        byte[] bytes = new byte[byteCount];
        i.read(bytes);
        return bytes;
    }

    public static final void writeByte(byte v, OutputStream o) throws IOException {
        o.write(v);
        o.flush();
    }

    public static final byte readByte(InputStream i) throws IOException {
        return (byte) i.read();
    }
}
