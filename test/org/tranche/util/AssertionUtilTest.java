/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tranche.util;

import java.io.File;
import junit.framework.TestCase;
import org.tranche.exceptions.AssertionFailedException;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class AssertionUtilTest extends TrancheTestCase {

    public AssertionUtilTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of throwExceptionIfAnyNull method, of class AssertionUtil.
     */
    public void testThrowExceptionIfAnyNull_Object() throws Exception {
        TestUtil.printTitle("AssertionUtilTest:testThrowExceptionIfAnyNull_Object");
        Object o = null;

        try {
            AssertionUtil.assertNoNullValues(o);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        o = new Object();
        AssertionUtil.assertNoNullValues(o);
    }

    /**
     * Test of throwExceptionIfAnyNull method, of class AssertionUtil.
     */
    public void testThrowExceptionIfAnyNull_ObjectArr() throws Exception {
        TestUtil.printTitle("AssertionUtilTest:testThrowExceptionIfAnyNull_ObjectArr");
        Object[] arg = null;


        try {
            AssertionUtil.assertNoNullValues(arg);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        arg = new Object[0];
        AssertionUtil.assertNoNullValues(arg);

        Object[] arg2 = {
            new Object(),
            null,
            new Object()
        };
        try {
            AssertionUtil.assertNoNullValues(arg2);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }
    }

    /**
     * Test of throwExceptionIfAnyNull method, of class AssertionUtil.
     */
    public void testThrowExceptionIfAnyNull_byteArr() throws Exception {
        TestUtil.printTitle("AssertionUtilTest:testThrowExceptionIfAnyNull_byteArr");
        byte[] bytes = null;
        try {
            AssertionUtil.assertNoNullValues(bytes);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        bytes = new byte[0];
        AssertionUtil.assertNoNullValues(bytes);
    }

    /**
     * Test of throwExceptionIfAnyNull method, of class AssertionUtil.
     */
    public void testThrowExceptionIfAnyNull_byteArrArr() throws Exception {
        TestUtil.printTitle("AssertionUtilTest:testThrowExceptionIfAnyNull_byteArrArr");
        byte[][] bytes = null;

        try {
            AssertionUtil.assertNoNullValues(bytes);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        byte[][] bytes2 = {
            null,
            new byte[0]
        };

        try {
            AssertionUtil.assertNoNullValues(bytes2);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        byte[][] bytes3 = {
            new byte[0],
            new byte[0]
        };
        AssertionUtil.assertNoNullValues(bytes3);
    }

    /**
     * Test of throwExceptionIfAnyNull method, of class AssertionUtil.
     */
    public void testThrowExceptionIfAnyNull_ObjectArrArr() throws Exception {
        TestUtil.printTitle("AssertionUtilTest:testThrowExceptionIfAnyNull_ObjectArrArr");
        Object[][] arg = null;

        try {
            AssertionUtil.assertNoNullValues(arg);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        Object[][] arg2 = {
            new Object[0],
            null
        };

        try {
            AssertionUtil.assertNoNullValues(arg2);
            fail("Should have throw npe.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        Object[][] arg3 = {
            new Object[0],
            new Object[0]
        };

        AssertionUtil.assertNoNullValues(arg3);
    }

    /**
     * Test of throwExceptionIfDuplication method, of class AssertionUtil.
     */
    public void testThrowExceptionIfDuplication() throws Exception {
        TestUtil.printTitle("AssertionUtilTest:testThrowExceptionIfDuplication");

        String[] arr1 = {
            "bryan",
            "augie",
            "mark",
            "bryan"
        };

        try {
            AssertionUtil.assertNoDuplicateObjects(arr1);
            fail("Should have throw exception.");
        } catch (AssertionFailedException npe) {
            // expected
        }

        String[] arr2 = {
            "bryan",
            "augie",
            "mark"
        };

        AssertionUtil.assertNoDuplicateObjects(arr2);
    }

    public void testAssertSameDirectoriesSame() throws Exception {
        File dir = null;
        try {
            final int numFilesInProject = RandomUtil.getInt(3) + 1;
            final int max = 1024;
            dir = DevUtil.createTestProject(numFilesInProject, 0, max);
            assertEquals("Expecting certain number of files.", numFilesInProject, dir.list().length);
            AssertionUtil.assertSame(dir, dir);
        } finally {
            IOUtil.recursiveDeleteWithWarning(dir);
        }
    }

    public void testAssertSameDirectoriesDifferent() throws Exception {
        File dir1 = null, dir2 = null;
        try {
            dir1 = DevUtil.createTestProject(RandomUtil.getInt(3) + 1, 0, 1024 * 1024);
            dir2 = DevUtil.createTestProject(RandomUtil.getInt(3) + 1, 0, 1024 * 1024);
            try {
                AssertionUtil.assertSame(dir1, dir2);
                fail("Should have thrown an AssertionFailedException");
            } catch (AssertionFailedException e) {
                // Expected
            }
        } finally {
            IOUtil.recursiveDeleteWithWarning(dir1);
            IOUtil.recursiveDeleteWithWarning(dir2);
        }
    }

    public void testAssertSameFilesSame() throws Exception {
        File f = null;
        try {
            f = DevUtil.createTestFile(0, 1024 * 1024);
            AssertionUtil.assertSame(f, f);
        } finally {
            IOUtil.safeDelete(f);
        }
    }

    public void testAssertSameFilesDifferent() throws Exception {
        File f1 = null, f2 = null;
        try {
            // Make a moderately large min to decrease odds of randomly creating same file
            final int min = 1024*16;
            f1 = DevUtil.createTestFile(min, 1024*1024);
            f2 = DevUtil.createTestFile(min, 1024*1024);
            
            try {
                AssertionUtil.assertSame(f1, f2);
                fail("Should have thrown an AssertionFailedException");
            } catch (AssertionFailedException e) {
                // Expected
            }
        } finally {
            IOUtil.safeDelete(f1);
            IOUtil.safeDelete(f2);
        }
    }

    public void testAssertSameFileVersusDirectory() throws Exception {
        File file = null, dir = null;
        try {
            // Make test efficient. Max size shouldn't matter much.
            int max = 1024*16;
            file = DevUtil.createTestFile(0, max);
            dir = DevUtil.createTestProject(RandomUtil.getInt(3), 0, max);
            
            try {
                AssertionUtil.assertSame(file, dir);
                fail("Should have thrown an AssertionFailedException");
            } catch (AssertionFailedException e) {
                // Expected
            }
        } finally {
            IOUtil.safeDelete(file);
            IOUtil.safeDelete(dir);
        }
    }
}
