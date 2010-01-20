/*
 * StressIOUtil.java
 *
 * Created on October 6, 2007, 7:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.tranche.TrancheServer;
import org.tranche.remote.RemoteTrancheServer;
import org.tranche.util.IOUtil;

/**
 * Adds a connect to use one RemoteTrancheServer per thread.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class StressIOUtil extends IOUtil {
    
    private static Map<Thread,RemoteTrancheServer> stressCache = new HashMap();
    
    /**
     * Closes and clears out cache.
     */
    public static void clearCache() {
        RemoteTrancheServer.setMultiSocketTest(false);
        for (Thread t : stressCache.keySet()) {
            RemoteTrancheServer rts = stressCache.get(t);
            IOUtil.safeClose(rts);
        }
        RemoteTrancheServer.setMultiSocketTest(true);
        stressCache.clear();
    }
    
    /**
     * Over-ride for stress testing. Only supports tranche://
     */
    public static synchronized TrancheServer connect(String url, Thread caller) throws Exception {
        // null check
        if (url == null) {
            throw new IOException("Can't pass a null URL.");
        }
        
        // trim the url
        url = url.trim();
        
        // handle unsecure remote connections
        if (url.startsWith("tranche://")) {
            // fall back on making a new connection
            String sub = url.split("tranche://")[1];
            String[] parts = sub.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            
            // try to get the cache from the pool
            RemoteTrancheServer cache = null;
            synchronized(remoteCache) {
                cache = stressCache.get(caller);
            }
            
            // if null, add a new cache entry
            if (cache == null) {
                cache = new RemoteTrancheServer(host, port, false);
                synchronized (remoteCache) {
                    System.out.println("New socket connection, total of "+getSocketCount());
                    stressCache.put(caller, cache);
                }
            }
            return cache;
        }
        
        // fall back on throwing an exception
        throw new IOException("Can not connect to URL " + url + ". Unknown protocol or host.");
    }
    
    public static int getSocketCount() {
        return stressCache.size();
    }
}
