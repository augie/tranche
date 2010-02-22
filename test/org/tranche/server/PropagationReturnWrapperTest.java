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
package org.tranche.server;

import java.util.HashSet;
import java.util.Set;
import org.tranche.exceptions.ServerIsNotWritableException;
import org.tranche.util.RandomUtil;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class PropagationReturnWrapperTest extends TrancheTestCase {

    /**
     * <p>Helper method.</p>
     * @return
     */
    private static Set<PropagationExceptionWrapper> getThreeExceptions() {
        Set<PropagationExceptionWrapper> set = new HashSet();

        String w1Host = "bryanesmith.com";
        String w1Message = "Testing #1 (contains a comma, and a colon:)";
        PropagationExceptionWrapper w1 = new PropagationExceptionWrapper(new Exception(w1Message), w1Host);

        String w2Host = "828productions.com";
        String w2Message = "Testing #2 :,,,,,,: Just testing\n\nsome content!~";
        PropagationExceptionWrapper w2 = new PropagationExceptionWrapper(new RuntimeException(w2Message), w2Host);

        String w3Host = "w3.org";
        PropagationExceptionWrapper w3 = new PropagationExceptionWrapper(new ServerIsNotWritableException(), w3Host);

        set.add(w1);
        set.add(w2);
        set.add(w3);

        return set;
    }

    /**
     * <p>Test all return types when no exceptions. Byte arrays are initialized to non-empty, random dimension lengths.</p>
     * @throws java.lang.Exception
     */
    public void testSimpleNoExceptions() throws Exception {
        TestUtil.printTitle("PropagationReturnWrapperTest.testSimpleNoExceptions");

        // -----------------------------------------------------------------------------------------
        // Void
        // -----------------------------------------------------------------------------------------
        PropagationReturnWrapper w1 = new PropagationReturnWrapper(new HashSet());
        byte[] w1Bytes = w1.toByteArray();
        PropagationReturnWrapper w1Verify = PropagationReturnWrapper.createFromBytes(w1Bytes);

        assertFalse("Shouldn't be any errors.", w1Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w1Verify.getErrors().size());

        assertTrue("See code for more information.", w1Verify.isVoid());
        assertTrue("See code for more information.", !w1Verify.isBoolean());
        assertTrue("See code for more information.", !w1Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w1Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w1Verify.isByteArrayTripleDimension());

        // -----------------------------------------------------------------------------------------
        // Boolean
        // -----------------------------------------------------------------------------------------
        PropagationReturnWrapper w2 = new PropagationReturnWrapper(new HashSet(), Boolean.TRUE);
        byte[] w2Bytes = w2.toByteArray();
        PropagationReturnWrapper w2Verify = PropagationReturnWrapper.createFromBytes(w2Bytes);

        assertFalse("Shouldn't be any errors.", w2Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w2Verify.getErrors().size());

        assertTrue("See code for more information.", !w2Verify.isVoid());
        assertTrue("See code for more information.", w2Verify.isBoolean());
        assertTrue("See code for more information.", !w2Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w2Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w2Verify.isByteArrayTripleDimension());

        assertTrue("Should anticipate boolean return code.", (Boolean) w2Verify.getReturnValueObject());

        PropagationReturnWrapper w3 = new PropagationReturnWrapper(new HashSet(), Boolean.FALSE);
        byte[] w3Bytes = w3.toByteArray();
        PropagationReturnWrapper w3Verify = PropagationReturnWrapper.createFromBytes(w3Bytes);

        assertFalse("Shouldn't be any errors.", w3Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w3Verify.getErrors().size());

        assertTrue("See code for more information.", !w3Verify.isVoid());
        assertTrue("See code for more information.", w3Verify.isBoolean());
        assertTrue("See code for more information.", !w3Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w3Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w3Verify.isByteArrayTripleDimension());

        assertFalse("Should anticipate boolean return code.", (Boolean) w3Verify.getReturnValueObject());

        // -----------------------------------------------------------------------------------------
        // 1d binary array
        // -----------------------------------------------------------------------------------------
        byte[] val1D = new byte[RandomUtil.getInt(10) + 1];
        RandomUtil.getBytes(val1D);

        PropagationReturnWrapper w4 = new PropagationReturnWrapper(new HashSet(), val1D);
        assertTrue("See code for more information.", !w4.isVoid());
        assertTrue("See code for more information.", !w4.isBoolean());
        assertTrue("See code for more information.", w4.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w4.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w4.isByteArrayTripleDimension());
        byte[] w4Bytes = w4.toByteArray();
        PropagationReturnWrapper w4Verify = PropagationReturnWrapper.createFromBytes(w4Bytes);

        assertFalse("Shouldn't be any errors.", w4Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w4Verify.getErrors().size());

        assertTrue("See code for more information.", !w4Verify.isVoid());
        assertTrue("See code for more information.", !w4Verify.isBoolean());
        assertTrue("See code for more information.", w4Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w4Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w4Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[] val1DVerify = (byte[]) w4Verify.getReturnValueObject();

        assertEquals("Should be same length.", val1D.length, val1DVerify.length);

        for (int i = 0; i < val1DVerify.length; i++) {
            assertEquals("Should be equal bytes.", val1D[i], val1DVerify[i]);
        }

        // -----------------------------------------------------------------------------------------
        // 2d binary array
        // -----------------------------------------------------------------------------------------
        byte[][] val2D = new byte[RandomUtil.getInt(10) + 1][RandomUtil.getInt(10) + 1];
        for (int i = 0; i < val2D.length; i++) {
            RandomUtil.getBytes(val2D[i]);
        }

        PropagationReturnWrapper w5 = new PropagationReturnWrapper(new HashSet(), val2D);
        byte[] w5Bytes = w5.toByteArray();
        PropagationReturnWrapper w5Verify = PropagationReturnWrapper.createFromBytes(w5Bytes);

        assertFalse("Shouldn't be any errors.", w5Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w5Verify.getErrors().size());

        assertTrue("See code for more information.", !w5Verify.isVoid());
        assertTrue("See code for more information.", !w5Verify.isBoolean());
        assertTrue("See code for more information.", !w5Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", w5Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w5Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[][] val2DVerify = (byte[][]) w5Verify.getReturnValueObject();

        assertEquals("Should be same length.", val2D.length, val2DVerify.length);

        for (int i = 0; i < val2DVerify.length; i++) {
            assertEquals("Should be same length.", val2D[i].length, val2DVerify[i].length);
            for (int j = 0; j < val2DVerify[i].length; j++) {
                assertEquals("Should be equal bytes.", val2D[i][j], val2DVerify[i][j]);
            }
        }

        // -----------------------------------------------------------------------------------------
        // 3d binary array
        // -----------------------------------------------------------------------------------------
        byte[][][] val3D = new byte[RandomUtil.getInt(10) + 1][RandomUtil.getInt(10) + 1][RandomUtil.getInt(10) + 1];
        for (int i = 0; i < val3D.length; i++) {
            for (int j = 0; j < val3D[i].length; j++) {
               RandomUtil.getBytes(val3D[i][j]);
            }
        }

        PropagationReturnWrapper w6 = new PropagationReturnWrapper(new HashSet(), val3D);
        byte[] w6Bytes = w6.toByteArray();
        PropagationReturnWrapper w6Verify = PropagationReturnWrapper.createFromBytes(w6Bytes);

        assertFalse("Shouldn't be any errors.", w6Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w6Verify.getErrors().size());

        assertTrue("See code for more information.", !w6Verify.isVoid());
        assertTrue("See code for more information.", !w6Verify.isBoolean());
        assertTrue("See code for more information.", !w6Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w6Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", w6Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[][][] val3DVerify = (byte[][][]) w6Verify.getReturnValueObject();

        assertEquals("Should be same length.", val3D.length, val3DVerify.length);

        for (int i = 0; i < val3DVerify.length; i++) {
            assertEquals("Should be same length.", val3D[i].length, val3DVerify[i].length);
            for (int j = 0; j < val3DVerify[i].length; j++) {
                assertEquals("Should be same length.", val3D[i][j].length, val3DVerify[i][j].length);
                for (int k = 0; k < val3DVerify[i][j].length; k++) {
                    assertEquals("Should be equal bytes.", val3D[i][j][k], val3DVerify[i][j][k]);
                }
            }
        }
    }

    /**
     * <p>Test all return types when three exceptions. Byte arrays are initialized to non-empty, random dimension lengths.</p>
     * @throws java.lang.Exception
     */
    public void testSimpleThreeExceptions() throws Exception {        
        TestUtil.printTitle("PropagationReturnWrapperTest.testSimpleThreeExceptions");

        // -----------------------------------------------------------------------------------------
        // Void
        // -----------------------------------------------------------------------------------------
        PropagationReturnWrapper w1 = new PropagationReturnWrapper(getThreeExceptions());
        byte[] w1Bytes = w1.toByteArray();
        PropagationReturnWrapper w1Verify = PropagationReturnWrapper.createFromBytes(w1Bytes);

        assertTrue("Should be errors.", w1Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 3, w1Verify.getErrors().size());

        assertTrue("See code for more information.", w1Verify.isVoid());
        assertTrue("See code for more information.", !w1Verify.isBoolean());
        assertTrue("See code for more information.", !w1Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w1Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w1Verify.isByteArrayTripleDimension());

        // -----------------------------------------------------------------------------------------
        // Boolean
        // -----------------------------------------------------------------------------------------
        PropagationReturnWrapper w2 = new PropagationReturnWrapper(getThreeExceptions(), Boolean.TRUE);
        byte[] w2Bytes = w2.toByteArray();
        PropagationReturnWrapper w2Verify = PropagationReturnWrapper.createFromBytes(w2Bytes);

        assertTrue("Should be errors.", w2Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 3, w2Verify.getErrors().size());

        assertTrue("See code for more information.", !w2Verify.isVoid());
        assertTrue("See code for more information.", w2Verify.isBoolean());
        assertTrue("See code for more information.", !w2Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w2Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w2Verify.isByteArrayTripleDimension());

        assertTrue("Should anticipate boolean return code.", (Boolean) w2Verify.getReturnValueObject());

        PropagationReturnWrapper w3 = new PropagationReturnWrapper(getThreeExceptions(), Boolean.FALSE);
        byte[] w3Bytes = w3.toByteArray();
        PropagationReturnWrapper w3Verify = PropagationReturnWrapper.createFromBytes(w3Bytes);

        assertTrue("Should be errors.", w3Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 3, w3Verify.getErrors().size());

        assertTrue("See code for more information.", !w3Verify.isVoid());
        assertTrue("See code for more information.", w3Verify.isBoolean());
        assertTrue("See code for more information.", !w3Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w3Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w3Verify.isByteArrayTripleDimension());

        assertFalse("Should anticipate boolean return code.", (Boolean) w3Verify.getReturnValueObject());

        // -----------------------------------------------------------------------------------------
        // 1d binary array
        // -----------------------------------------------------------------------------------------
        byte[] val1D = new byte[RandomUtil.getInt(10) + 1];
       RandomUtil.getBytes(val1D);

        PropagationReturnWrapper w4 = new PropagationReturnWrapper(getThreeExceptions(), val1D);
        byte[] w4Bytes = w4.toByteArray();
        PropagationReturnWrapper w4Verify = PropagationReturnWrapper.createFromBytes(w4Bytes);

        assertTrue("Should be errors.", w4Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 3, w4Verify.getErrors().size());

        assertTrue("See code for more information.", !w4Verify.isVoid());
        assertTrue("See code for more information.", !w4Verify.isBoolean());
        assertTrue("See code for more information.", w4Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w4Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w4Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[] val1DVerify = (byte[]) w4Verify.getReturnValueObject();

        assertEquals("Should be same length.", val1D.length, val1DVerify.length);

        for (int i = 0; i < val1DVerify.length; i++) {
            assertEquals("Should be equal bytes.", val1D[i], val1DVerify[i]);
        }

        // -----------------------------------------------------------------------------------------
        // 2d binary array
        // -----------------------------------------------------------------------------------------
        byte[][] val2D = new byte[RandomUtil.getInt(10) + 1][RandomUtil.getInt(10) + 1];
        for (int i = 0; i < val2D.length; i++) {
            RandomUtil.getBytes(val2D[i]);
        }

        PropagationReturnWrapper w5 = new PropagationReturnWrapper(getThreeExceptions(), val2D);
        byte[] w5Bytes = w5.toByteArray();
        PropagationReturnWrapper w5Verify = PropagationReturnWrapper.createFromBytes(w5Bytes);

        assertTrue("Should be errors.", w5Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 3, w5Verify.getErrors().size());

        assertTrue("See code for more information.", !w5Verify.isVoid());
        assertTrue("See code for more information.", !w5Verify.isBoolean());
        assertTrue("See code for more information.", !w5Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", w5Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w5Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[][] val2DVerify = (byte[][]) w5Verify.getReturnValueObject();

        assertEquals("Should be same length.", val2D.length, val2DVerify.length);

        for (int i = 0; i < val2DVerify.length; i++) {
            assertEquals("Should be same length.", val2D[i].length, val2DVerify[i].length);
            for (int j = 0; j < val2DVerify[i].length; j++) {
                assertEquals("Should be equal bytes.", val2D[i][j], val2DVerify[i][j]);
            }
        }

        // -----------------------------------------------------------------------------------------
        // 3d binary array
        // -----------------------------------------------------------------------------------------
        byte[][][] val3D = new byte[RandomUtil.getInt(10) + 1][RandomUtil.getInt(10) + 1][RandomUtil.getInt(10) + 1];
        for (int i = 0; i < val3D.length; i++) {
            for (int j = 0; j < val3D[i].length; j++) {
                RandomUtil.getBytes(val3D[i][j]);
            }
        }

        PropagationReturnWrapper w6 = new PropagationReturnWrapper(getThreeExceptions(), val3D);
        byte[] w6Bytes = w6.toByteArray();
        PropagationReturnWrapper w6Verify = PropagationReturnWrapper.createFromBytes(w6Bytes);

        assertTrue("Should be errors.", w6Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 3, w6Verify.getErrors().size());

        assertTrue("See code for more information.", !w6Verify.isVoid());
        assertTrue("See code for more information.", !w6Verify.isBoolean());
        assertTrue("See code for more information.", !w6Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w6Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", w6Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[][][] val3DVerify = (byte[][][]) w6Verify.getReturnValueObject();

        assertEquals("Should be same length.", val3D.length, val3DVerify.length);

        for (int i = 0; i < val3DVerify.length; i++) {
            assertEquals("Should be same length.", val3D[i].length, val3DVerify[i].length);
            for (int j = 0; j < val3DVerify[i].length; j++) {
                assertEquals("Should be same length.", val3D[i][j].length, val3DVerify[i][j].length);
                for (int k = 0; k < val3DVerify[i][j].length; k++) {
                    assertEquals("Should be equal bytes.", val3D[i][j][k], val3DVerify[i][j][k]);
                }
            }
        }
    }

    /**
     * <p>Tests what happens when a 1D byte array is empty.</p>
     * @throws java.lang.Exception
     */
    public void test1DByteArrayEmpty() throws Exception {
        
        TestUtil.printTitle("PropagationReturnWrapperTest.test1DByteArrayEmpty");
        
        byte[] empty1D = new byte[0];
        PropagationReturnWrapper w1 = new PropagationReturnWrapper(new HashSet(), empty1D);
        byte[] w1Bytes = w1.toByteArray();
        PropagationReturnWrapper w1Verify = PropagationReturnWrapper.createFromBytes(w1Bytes);

        assertFalse("Shouldn't be any errors.", w1Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w1Verify.getErrors().size());

        assertTrue("See code for more information.", !w1Verify.isVoid());
        assertTrue("See code for more information.", !w1Verify.isBoolean());
        assertTrue("See code for more information.", w1Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", !w1Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w1Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[] val1DVerify = (byte[]) w1Verify.getReturnValueObject();
        assertEquals("Should be empty.", 0, val1DVerify.length);
    }

    /**
     * <p>Tests what happens when a 2D byte array is empty or has empty dimensions.</p>
     * @throws java.lang.Exception
     */
    public void test2DByteArrayEmpty() throws Exception {
        
        TestUtil.printTitle("PropagationReturnWrapperTest.test2DByteArrayEmpty");
        
        byte[][] empty2D = new byte[0][];
        PropagationReturnWrapper w1 = new PropagationReturnWrapper(new HashSet(), empty2D);
        byte[] w1Bytes = w1.toByteArray();
        PropagationReturnWrapper w1Verify = PropagationReturnWrapper.createFromBytes(w1Bytes);

        assertFalse("Shouldn't be any errors.", w1Verify.isAnyErrors());
        assertEquals("Expecting certain number of exceptions.", 0, w1Verify.getErrors().size());

        assertTrue("See code for more information.", !w1Verify.isVoid());
        assertTrue("See code for more information.", !w1Verify.isBoolean());
        assertTrue("See code for more information.", !w1Verify.isByteArraySingleDimension());
        assertTrue("See code for more information.", w1Verify.isByteArrayDoubleDimension());
        assertTrue("See code for more information.", !w1Verify.isByteArrayTripleDimension());

        // Check the bytes
        byte[][] val2DVerify = (byte[][]) w1Verify.getReturnValueObject();
        assertEquals("Should be empty.", 0, val2DVerify.length);
    }

    /**
     * <p>Tests what happens when a 3D byte array is empty or has empty dimensions.</p>
     * @throws java.lang.Exception
     */
    public void test3DByteArrayEmpty() throws Exception {
        TestUtil.printTitle("PropagationReturnWrapperTest.test3DByteArrayEmpty");
        {
            byte[][][] empty3D = new byte[0][][];
            PropagationReturnWrapper w1 = new PropagationReturnWrapper(new HashSet(), empty3D);
            byte[] w1Bytes = w1.toByteArray();
            PropagationReturnWrapper w1Verify = PropagationReturnWrapper.createFromBytes(w1Bytes);

            assertFalse("Shouldn't be any errors.", w1Verify.isAnyErrors());
            assertEquals("Expecting certain number of exceptions.", 0, w1Verify.getErrors().size());

            assertTrue("See code for more information.", !w1Verify.isVoid());
            assertTrue("See code for more information.", !w1Verify.isBoolean());
            assertTrue("See code for more information.", !w1Verify.isByteArraySingleDimension());
            assertTrue("See code for more information.", !w1Verify.isByteArrayDoubleDimension());
            assertTrue("See code for more information.", w1Verify.isByteArrayTripleDimension());

            // Check the bytes
            byte[][][] val3DVerify = (byte[][][]) w1Verify.getReturnValueObject();
            assertEquals("Should be empty.", 0, val3DVerify.length);
        }
        {
            byte[][][] empty3D = new byte[8][0][];
            PropagationReturnWrapper w1 = new PropagationReturnWrapper(new HashSet(), empty3D);
            byte[] w1Bytes = w1.toByteArray();
            PropagationReturnWrapper w1Verify = PropagationReturnWrapper.createFromBytes(w1Bytes);

            assertFalse("Shouldn't be any errors.", w1Verify.isAnyErrors());
            assertEquals("Expecting certain number of exceptions.", 0, w1Verify.getErrors().size());

            assertTrue("See code for more information.", !w1Verify.isVoid());
            assertTrue("See code for more information.", !w1Verify.isBoolean());
            assertTrue("See code for more information.", !w1Verify.isByteArraySingleDimension());
            assertTrue("See code for more information.", !w1Verify.isByteArrayDoubleDimension());
            assertTrue("See code for more information.", w1Verify.isByteArrayTripleDimension());

            // Check the bytes
            byte[][][] val3DVerify = (byte[][][]) w1Verify.getReturnValueObject();
            assertEquals("Should be empty.", 8, val3DVerify.length);
            assertEquals("Should be empty.", 0, val3DVerify[0].length);
            assertEquals("Should be empty.", 0, val3DVerify[1].length);
            assertEquals("Should be empty.", 0, val3DVerify[2].length);
            assertEquals("Should be empty.", 0, val3DVerify[3].length);
            assertEquals("Should be empty.", 0, val3DVerify[4].length);
            assertEquals("Should be empty.", 0, val3DVerify[5].length);
            assertEquals("Should be empty.", 0, val3DVerify[6].length);
            assertEquals("Should be empty.", 0, val3DVerify[7].length);
        }
        
        {
            byte[][][] empty3D = new byte[3][2][0];
            PropagationReturnWrapper w1 = new PropagationReturnWrapper(new HashSet(), empty3D);
            byte[] w1Bytes = w1.toByteArray();
            PropagationReturnWrapper w1Verify = PropagationReturnWrapper.createFromBytes(w1Bytes);

            assertFalse("Shouldn't be any errors.", w1Verify.isAnyErrors());
            assertEquals("Expecting certain number of exceptions.", 0, w1Verify.getErrors().size());

            assertTrue("See code for more information.", !w1Verify.isVoid());
            assertTrue("See code for more information.", !w1Verify.isBoolean());
            assertTrue("See code for more information.", !w1Verify.isByteArraySingleDimension());
            assertTrue("See code for more information.", !w1Verify.isByteArrayDoubleDimension());
            assertTrue("See code for more information.", w1Verify.isByteArrayTripleDimension());

            // Check the bytes
            byte[][][] val3DVerify = (byte[][][]) w1Verify.getReturnValueObject();
            assertEquals("Should be empty.", 3, val3DVerify.length);
            assertEquals("Should be empty.", 2, val3DVerify[0].length);
            assertEquals("Should be empty.", 2, val3DVerify[1].length);
            assertEquals("Should be empty.", 2, val3DVerify[2].length);
            
            for (int i=0; i<3; i++) {
                for (int j=0; j<2; j++) {
                    assertEquals("Should be empty.",0, val3DVerify[i][j].length);
                }
            }
        }
    }
}
