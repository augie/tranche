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

import java.io.File;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.tranche.servers.ServerUtil;
import org.tranche.users.UserZipFile;

/**
 * A utility used to stress a server.
 *
 * NOTE: to run the stress test, will need to place stress_auth.zip.encrypted in your home directory
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class StressUtil {
    
    private final static String HOME_DIR = System.getProperty("user.home") + System.getProperty("file.separator");
    private static UserZipFile user;
    
    /**
     * Used to run simple tests on the stress test itself. Not intended as actual stress test.
     */
    
    public static void main(String[] args) {
        
        stressServer(100, "tranche://127.0.0.1:1500");
        
    }
    
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    
    /**
     * <p>Perform a stress test on a server. More connections means more stress.</p>
     * @param connections Number of concurrent connections to make
     * @param url The url for the tranche server with protocol and port (e.g., tranche://127.0.0.1:443)
     */
    
    public static void stressServer(int connections, String url) {
        
        if (!ServerUtil.isServerOnline(url)) {
            System.out.println("\n\nERROR: Test server not online at " + url + "\n\n");
            return;
        }
        
        // Get user information (must be ~/stress_auth.zip.encrypted)
        user = new UserZipFile(new File(HOME_DIR + "stress_auth.zip.encrypted"));
        user.setPassphrase("");
        
        ArrayBlockingQueue queue = new ArrayBlockingQueue(1000);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(100,100,Long.MAX_VALUE,TimeUnit.MILLISECONDS,queue);
        
        // Create and start the log
        StressLog log = new StressLog(connections);
        log.start();
        
        // Create and submit a connection thread for each desired connection
        for (int i = 0; i < connections; i++) {
            
            StressConnection next = new StressConnection(i, url, user.getCertificate(), user.getPrivateKey(), log);
            executor.submit(next);
        }
        
        executor.shutdown();
        
        while(!executor.isTerminated())
            try {
                executor.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                System.out.println("For some reason, the executor can't wait... " + ex.getMessage());
            }
        
        // Stop log so it will print results
        log.finish();
        
    } // stressServer
    
    public static byte[] makeRandomData(int length) {
        byte[] data = new byte[length];
        Random random = new Random();
        random.nextBytes(data);
        return data;
    }
}
