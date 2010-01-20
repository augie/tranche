/*
 * Configuration.java
 *
 * Created on October 5, 2007, 4:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package stress.server;

import org.tranche.flatfile.FlatFileTrancheServer;


/**
 * Shove any configuration information here.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class ServerConfiguration {
    
    /**
     * Port to bind the Tranche Server instance.
     */
    public static final int TRANCHE_SERVER_PORT = 1500;
    
    /**
     * Port to bind the StressServer instance, used for remotely starting/stopping server.
     */
    public static final int STRESS_SERVER_PORT = 1600;
    
    /**
     * Data directory for the FlatFileTrancheServer. Put this somewhere where there is a lot of room
     */
    public static final String DATA_DIRECTORY_PATH = FlatFileTrancheServer.getDefaultHomeDir();
}
