
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.tranche.TrancheServer;
import org.tranche.hash.BigHash;
import org.tranche.servers.ServerUtil;
import org.tranche.util.IOUtil;

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

/**
 * Class represents single connection for server stress test
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class StressConnection extends Thread {
    
    String url; // URL for server (e.g., tranche://127.0.0.1:1500)
    int id; // Number of connections. An id for the thread.
    StressLog log; // Object logs activities
    
    X509Certificate cert;
    PrivateKey key;
    
    TrancheServer server = null;
    
    public StressConnection(int id, String url, X509Certificate cert, PrivateKey key, StressLog log) {
        
        super("Tranche Server Stress Test Connection #" + id);
        
        this.id = id;
        this.url = url;
        this.log = log;
        this.cert = cert;
        this.key = key;
    }
    
    // Go! Next connection...
    public void run() {
        
        try {
            
            ServerUtil.waitForStartup();
            
            server = IOUtil.connect(url);
        }
        
        catch (Exception e) {
            System.out.println("Exception in thread " + this.id + ": " + e.getMessage());
            
            // Can do much if can't connect
            return;
        }
        
        long start = 0;
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 1. Get nonce
        
        try {
            log.attempt(log.GET_NONCE);
            start = System.currentTimeMillis();
            byte[] none = server.getNonce();
            log.pass(log.GET_NONCE, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.GET_NONCE, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 2. Get config
        try {
            log.attempt(log.GET_CONFIG);
            start = System.currentTimeMillis();
            IOUtil.getConfiguration(server, cert, key);
            log.pass(log.GET_CONFIG, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            System.out.println("UH-OH, DOESN'T RECOGNIZE USER!!!");
            System.out.println(e.getMessage());
            log.fail(log.GET_CONFIG, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 3. Set data
        BigHash dataHash = null;
        
        try {
            log.attempt(log.SET_DATA);
            start = System.currentTimeMillis();
            
            byte[] randomData = StressUtil.makeRandomData(8);
            dataHash = BigHash.createFromBytes(StressUtil.makeRandomData(76));
            IOUtil.setData(server, cert, key, dataHash, randomData);
            
            log.pass(log.SET_DATA, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            System.out.println("CAN'T SET DATA!!!");
            System.out.println(e.getMessage());
            log.fail(log.SET_DATA, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 4. Has data
        try {
            log.attempt(log.HAS_DATA);
            start = System.currentTimeMillis();
            
            boolean hasData = server.hasData(dataHash);
            
            log.pass(log.HAS_DATA, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.HAS_DATA, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 5. Get data
        try {
            log.attempt(log.GET_DATA);
            start = System.currentTimeMillis();
            
            // Dump data
            server.getData(dataHash);
            
            log.pass(log.GET_DATA, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.GET_DATA, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 6. Set meta
        BigHash metaHash = null;
        
        try {
            log.attempt(log.SET_META);
            start = System.currentTimeMillis();
            
            byte[] randomData = StressUtil.makeRandomData(8);
            metaHash = BigHash.createFromBytes(StressUtil.makeRandomData(76));
            IOUtil.setMetaData(server, cert, key, metaHash, randomData);
            
            log.pass(log.SET_META, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.SET_META, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 7. Has meta
        try {
            log.attempt(log.HAS_META);
            start = System.currentTimeMillis();
            
            boolean hasMeta = server.hasMetaData(metaHash);
            
            log.pass(log.HAS_META, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.HAS_META, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 8. Get meta
        try {
            log.attempt(log.GET_META);
            start = System.currentTimeMillis();
            
            // Dump data
            server.getMetaData(metaHash);
            
            log.pass(log.GET_META, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.GET_META, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 9. Delete data
        try {
            log.attempt(log.DELETE_DATA);
            start = System.currentTimeMillis();
            
            IOUtil.deleteData(server, cert, key, dataHash);
            
            log.pass(log.DELETE_DATA, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.DELETE_DATA, (System.currentTimeMillis() - start));
        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 10. Delete meta
        
        // Delete meta not currently working. Suspect deals with tokens or wrong
        // callback item. Investigate in meeting.
//        try {
//            log.attempt(log.DELETE_META);
//            start = System.currentTimeMillis();
//            
//            IOUtil.deleteMetaData(server, cert, key, metaHash);
//            
//            log.pass(log.DELETE_META, (System.currentTimeMillis() - start));
//        }
//        
//        catch (Exception e) {
//            System.out.println(e.getMessage());
//            log.fail(log.DELETE_META, (System.currentTimeMillis() - start));
//        }
        
        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
        // 11. Get hashes
        try {
            log.attempt(log.GET_HASHES);
            start = System.currentTimeMillis();
            
            server.getDataHashes(BigInteger.ZERO, BigInteger.TEN);
            
            log.pass(log.GET_HASHES, (System.currentTimeMillis() - start));
        }
        
        catch (Exception e) {
            log.fail(log.GET_HASHES, (System.currentTimeMillis() - start));
        }
        
        finally {
            if (server != null)
                IOUtil.safeClose(server);
        }
        
    }
    
} // StressConnection
