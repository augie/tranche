/*
 * RemoteStressServer.java
 *
 * Created on October 5, 2007, 4:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import org.tranche.util.IOUtil;

/**
 * Asks the stress server to start and stop Tranche servers. (Remote control.)
 * @author Bryan Smith <bryanesmith at gmail dot com>
 */
public class RemoteStressServer {
    
    private String ip = null;
    
    /**
     * Create a remove stress server. Do not need to specify ports for servers: see Configuration.
     * @param ip The ip address of server, e.g., 192.168.1.5
     */
    public RemoteStressServer(String ip) {
        this.ip = ip;
    }
    
    /**
     * <p>Start a server for a test. Server will run until stopped.</p>
     */
    public void startTest() throws Exception {
        sendMessage(Protocol.REQUEST_START_TEST);
    }
    
    /**
     * <p>Stop a server. All this really does is delete all data from test and disconnect.</p>
     * <p>If want to run test with multiple client, simply don't call stop test and manually shut down server!</p>
     */
    public void stopTest() throws Exception {
        sendMessage(Protocol.REQUEST_STOP_TEST);
    }
    
    /**
     * <p>Ends both client and server.</p>
     */
    public void close() throws Exception {
        sendMessage(Protocol.REQUEST_DISCONNECT);
    }
    
    /**
     * 
     * @throws java.lang.Exception
     */
    public void turnOnDataBlockCache() throws Exception {
        sendMessage(Protocol.REQUEST_USE_DATA_BLOCK_CACHE);
    }
    
    /**
     * 
     * @throws java.lang.Exception
     */
    public void turnOffDataBlockCache() throws Exception {
        sendMessage(Protocol.REQUEST_NO_DATA_BLOCK_CACHE);
    }
    
    /**
     * Sends a request to StressServer. Protected access so can subclass for tests, etc.
     */
    protected void sendMessage(String message) throws Exception {
        Socket s = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        
        try {
            System.out.println("SENDING MESSAGE TO <"+this.ip+":"+ServerConfiguration.STRESS_SERVER_PORT+">: "+message.trim());
            s = new Socket(this.ip,ServerConfiguration.STRESS_SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                
            // Send command 
            writer.write(message);
            writer.flush();
            
            // Wait for response
            String response = reader.readLine();
            
            // If response is an error, throw Exception
            if (response.equals(Protocol.RESPONSE_ERROR.trim())) {
                throw new IOException("Server reply says there was an error.");
            }
            
            // Otherwise make sure the response token wasn't mangled
            if (!response.equals(Protocol.RESPONSE_OK.trim())) {
                throw new IOException("Server response not recognized: "+response);
            }
            
            System.out.println("CLIENT> received message: "+response);
        } finally {
            IOUtil.safeClose(writer);
            IOUtil.safeClose(reader);
            if (s != null)
                s.close();
        }
    }
}
