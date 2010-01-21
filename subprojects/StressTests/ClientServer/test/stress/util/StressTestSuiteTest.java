/*
 * StressTestSuiteTest.java
 * JUnit based test
 *
 * Created on October 6, 2007, 10:15 PM
 */

package stress.util;

import junit.framework.*;

/**
 * Not a recursive test -- uses whatever is the conf file at the moment.
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class StressTestSuiteTest extends TestCase {
    
    public void testNothing() {
        // Keep JUnit happy
    }
    
    public void testPrintWhateverIsCurrentInConfig() throws Exception {
        while (StressTestSuite.hasNext())
            System.out.println(StressTestSuite.getNext());
    }
}
