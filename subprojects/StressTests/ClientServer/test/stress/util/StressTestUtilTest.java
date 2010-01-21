/*
 * StressTestUtilTest.java
 * JUnit based test
 *
 * Created on October 6, 2007, 10:58 PM
 */

package stress.util;

import junit.framework.*;
import java.io.File;
import org.tranche.util.IOUtil;

/**
 *
 * @author Bryan E. Smith <bryanesmith at gmail dot com>
 */
public class StressTestUtilTest extends TestCase {
    
    public StressTestUtilTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testGetProjects() throws Exception {
        File proj1 = null, 
                proj2 = null, 
                proj3 = null;
        
        try {
            proj1 = StressTestUtil.createTestProject(10,1024*10);
            proj2 = StressTestUtil.createTestProject(3,1024*5);
            proj3 = StressTestUtil.createTestProject(3,1024*1024);
            
            assertTrue("Expect project to exist.",proj1.exists());
            assertEquals("Expecting certain number of files.",10,proj1.list().length);
            
            assertTrue("Expect project to exist.",proj2.exists());
            assertEquals("Expecting certain number of files.",3,proj2.list().length);
            
            assertTrue("Expect project to exist.",proj1.exists());
            assertEquals("Expecting certain number of files.",3,proj2.list().length);
            
        } finally {
            IOUtil.recursiveDeleteWithWarning(proj1);
            IOUtil.recursiveDeleteWithWarning(proj2);
            IOUtil.recursiveDeleteWithWarning(proj3);
        }
    }
    
}
