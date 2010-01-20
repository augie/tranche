/*
 * Protocol.java
 *
 * Created on October 5, 2007, 4:27 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.server;

import org.tranche.util.Text;


/**
 * Represents simple protocol between StressServer and RemoteStressServer.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class Protocol {
    
    // Client
    public static final String REQUEST_START_TEST = "START"+Text.getNewLine();
    public static final String REQUEST_STOP_TEST = "STOP"+Text.getNewLine();
    public static final String REQUEST_DISCONNECT = "DISCONNECT"+Text.getNewLine();
    
    // Server
    public static final String RESPONSE_OK = "OK"+Text.getNewLine();
    public static final String RESPONSE_ERROR = "ERROR"+Text.getNewLine();
    
}
