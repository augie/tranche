/*
 * RemoteStressServerTest.java
 * JUnit based test
 *
 * Created on October 5, 2007, 5:24 PM
 */

package stress.server;

import java.net.ConnectException;
import java.net.SocketException;
import junit.framework.*;

/**
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class RemoteStressServerTest extends TestCase {
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
        // Give a couple seconds to tear down servers
        Thread.sleep(2000);
    }
    
    /**
     * Test simple communication.
     */
    public void testSimpleInteractions() throws Exception {
        
        // Start a server
        Thread serverThread = new Thread("Stress test server thread") {
            public void run() {
                StressServer.main(new String[0]);
            }
        };
        // Low priority daemon for tests
        serverThread.setDaemon(true);
        serverThread.setPriority(Thread.MIN_PRIORITY);
        serverThread.start();
        
        // Let server start
        Thread.sleep(2000);
        
        RemoteStressServer rs = null;
        try {
            rs = new RemoteStressServer("127.0.0.1");
            
            rs.startTest();
            rs.stopTest();
            
            rs.startTest();
            rs.stopTest();
            
        } finally {
            rs.close();
        }
    }
    
    public void testDuplicate() throws Exception {
        // Run the last test again for bind exceptions, etc.
        testSimpleInteractions();
    }
    
    /**
     * Negative test. Start server, bad command.
     */
    public void testBadCommand() throws Exception {
        // Start a server
        Thread serverThread = new Thread("Stress test server thread") {
            public void run() {
                StressServer.main(new String[0]);
            }
        };
        // Low priority daemon for tests
        serverThread.setDaemon(true);
        serverThread.setPriority(Thread.MIN_PRIORITY);
        serverThread.start();
        
        // Let server start
        Thread.sleep(2000);
        
        NaughtyRemoteStressServer rs = null;
        try {
            rs = new NaughtyRemoteStressServer("127.0.0.1");
            
            rs.startTest();
            rs.stopTest();
            
            rs.startTest();
            rs.stopTest();
            try {
                rs.sendBadRequest();
                fail("Bad request should throw an exception");
            } catch (Exception ex) {
                // Negative test, this is what we want
            }
            
            rs.close();
            try {
                rs.stopTest();
                fail("Stress server should be closed, not accepting connections");
            } catch (SocketException se) {
                // Negative test , this is what we want
            }
            
        } finally {
            try {
                rs.close();
            } catch (Exception ex) { /* Ignore, may already be closed */ }
        }
    }
    
    class NaughtyRemoteStressServer extends RemoteStressServer {
        
        public NaughtyRemoteStressServer(String ip) {
            super(ip);
        }
        
        public void sendBadRequest() throws Exception {
            super.sendMessage("BAD_REQUEST\n");
        }
    }
}
