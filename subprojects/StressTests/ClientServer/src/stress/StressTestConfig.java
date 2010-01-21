/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stress;

import java.util.ArrayList;
import org.tranche.ConfigureTranche;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class StressTestConfig {
public static final String CONFIG_FILE_LOCATION = "/stress/stress2.conf";
    private static boolean loaded = false;

    /**
     * <p>Loads the configuration file for the ProteomeCommons.org Tranche network.</p>
     */
    public synchronized static void load() {
        // only load once
        if (loaded) {
            return;
        }
        loaded = true;
        ConfigureTranche.load(CONFIG_FILE_LOCATION);
    }

    /**
     * <p>Places the configuration file location at the front of an array of arguments.</p>
     * @param args
     * @return
     */
    public static String[] getArgsConfigAdjusted(String[] args) {
        ArrayList<String> argList = new ArrayList<String>();
        argList.add(CONFIG_FILE_LOCATION);
        try {
            for (String arg : args) {
                try {
                    argList.add(arg);
                } catch (Exception e) {
                    // null pointers
                }
            }
        } catch (Exception e) {
            // null pointers
        }
        return argList.toArray(new String[0]);
    }
}
