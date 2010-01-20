/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tranche.util;

import org.tranche.exceptions.AssertionFailedException;

/**
 * <p>Utility methods for common assertions.</p>
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class AssertionUtil {

    /**
     * <p>Throws exception if any null values found.</p>
     * @param o
     * @throws java.lang.Exception
     */
    public static void assertNoNullValues(Object o) throws Exception {
        if (o == null) {
            throw new AssertionFailedException("A null argument was received, but null values are not valid. (Object, null.)");
        }
    }

    /**
     * <p>Throws exception if any null values found.</p>
     * @param arg
     * @throws java.lang.Exception
     */
    public static void assertNoNullValues(Object[] arg) throws Exception {
        if (arg == null) {
            throw new AssertionFailedException("A null argument was received, but null values are not valid. (One-dimensional array, null.)");
        }
        for (int i = 0; i < arg.length; i++) {
            if (arg[i] == null) {
                throw new AssertionFailedException("A null argument was received, but null values are not valid. (One-dimensional array, i=" + i + ")");
            }
        }
    }

    /**
     * 
     * @param bytes
     * @throws java.lang.Exception
     */
    public static void assertNoNullValues(byte[] bytes) throws Exception {
        if (bytes == null) {
            throw new AssertionFailedException("A null argument was received, but null values are not valid. (One-dimension byte array, null.)");
        }
    }

    /**
     * 
     * @param bytes
     * @throws java.lang.Exception
     */
    public static void assertNoNullValues(byte[][] bytes) throws Exception {
        if (bytes == null) {
            throw new AssertionFailedException("A null argument was received, but null values are not valid. (One-dimension byte array, null.)");
        }
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == null) {
                throw new AssertionFailedException("A null argument was received, but null values are not valid. (Two-dimension byte array, i=" + i + ")");
            }
        }
    }

    /**
     * <p>Throws exception if any null values found.</p>
     * @param arg
     * @throws java.lang.Exception
     */
    public static void assertNoNullValues(Object[][] arg) throws Exception {

        if (arg == null) {
            throw new AssertionFailedException("A null argument was received, but null values are not valid. (Two-dimensional Object array, null.)");
        }

        for (int i = 0; i < arg.length; i++) {
            if (arg[i] == null) {
                throw new AssertionFailedException("A null argument was received, but null values are not valid. (Two-dimensional Object array, null at i=" + i + ")");
            }
            for (int j = 0; j < arg[i].length; j++) {
                if (arg[i][j] == null) {
                    throw new AssertionFailedException("A null argument was received, but null values are not valid. (Two-dimensional Object array, null at i=" + i + ", j=" + j + ")");
                }
            }
        }
    }

    /**
     * <p>Looks for any duplicates and throws an exception if found.</p>
     * @param arg
     * @throws java.lang.Exception
     */
    public static void assertNoDuplicateObjects(Object[] arg) throws Exception {
        I:
        for (int i = 0; i < arg.length; i++) {
            J:
            for (int j = 0; j < arg.length; j++) {
                if (i == j) {
                    continue J;
                }
                if (arg[i].equals(arg[j])) {
                    throw new AssertionFailedException("Duplicates not allowed for argument, but found duplicate values for: " + arg[i]);
                }
            }
        }
    }

    /**
     * <p>Assert that two byte arrays are of same size and have same contents.</p>
     * @param b1
     * @param b2
     * @throws java.lang.Exception
     */
    public static void assertBytesSame(byte[] b1, byte[] b2) throws Exception {
        if (b1 == null || b2 == null) {
            throw new AssertionFailedException("Parameter must not be null.");
        }
        if (b1.length != b2.length) {
            throw new AssertionFailedException("b1.length<" + b1.length + "> != b2.length<" + b2.length + ">");
        }

        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                throw new AssertionFailedException("b1[" + i + "]<" + b1[i] + "> != b2[" + i + "]<" + b2[i] + ">");
            }
        }
    }
    
    /**
     * <p>Assert that two byte arrays are of same size and have same contents.</p>
     * @param b1
     * @param b2
     * @throws java.lang.Exception
     */
    public static void assertBytesDifferent(byte[] b1, byte[] b2) throws Exception {
        if (b1 == null || b2 == null) {
            throw new AssertionFailedException("Parameter must not be null.");
        }
        
        // If different lengths, nothing else to check--different
        if (b1.length != b2.length) {
            return;
        }

        boolean same = true;
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                same = false;
                break;
            }
        }
        
        if (same) {
            throw new AssertionFailedException("Expecting bytes to differ, but are same.");
        }
    }
}
