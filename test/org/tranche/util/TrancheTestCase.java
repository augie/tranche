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
package org.tranche.util;

import junit.framework.TestCase;
import org.tranche.add.AddFileToolReport;
import org.tranche.get.GetFileToolReport;
import org.tranche.server.PropagationExceptionWrapper;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TrancheTestCase extends TestCase {

    private static final boolean isTesting = TestUtil.isTesting();
    
    public TrancheTestCase() {
        
    }
    
    public TrancheTestCase(String name) {
        super(name);
    }

    @Override()
    protected void setUp() throws Exception {
        TestUtil.setTesting(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        TempFileUtil.clear();
        TestUtil.setTesting(isTesting);
    }

    /**
     * <p>Assert that the upload completed successfully.</p>
     * @param report
     */
    protected void assertSuccess(AddFileToolReport report) {
        assertTrue("Upload should be finished.", report.isFinished());

        if (report.isFailed()) {
            System.err.println("The upload failed with the following " + report.getFailureExceptions().size() + " error(s):");
            for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                System.err.println("    * "+pew.exception.getMessage()+" <"+pew.host+">: "+pew.exception.getMessage());
                pew.exception.printStackTrace();
            }
            fail("Upload should not have failed, but did.");
        }
        
        assertNotNull("Hash should not be null.", report.getHash());
    }
    
    /**
     * <p>Assert that the download completed successfully.</p>
     * @param report
     */
    protected void assertSuccess(GetFileToolReport report) {
        assertTrue("Download should be finished.", report.isFinished());

        if (report.isFailed()) {
            System.err.println("The download failed with the following " + report.getFailureExceptions().size() + " error(s):");
            for (PropagationExceptionWrapper pew : report.getFailureExceptions()) {
                System.err.println("    * "+pew.exception.getMessage()+" <"+pew.host+">: "+pew.exception.getMessage());
                pew.exception.printStackTrace();
            }
            fail("Download should not have failed, but did.");
        }
    }
}
