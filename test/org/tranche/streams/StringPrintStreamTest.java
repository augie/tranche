/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tranche.streams;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class StringPrintStreamTest extends TrancheTestCase {

    public StringPrintStreamTest() {
    }

    public void testSimple() {
        StringPrintStream sps = StringPrintStream.create();
        sps.print("Hi!");
        assertEquals("Hi!", sps.getOutput());
        
        sps.println("Bye!");
        assertEquals("Hi!Bye!\n", sps.getOutput());
    }
    
    public void testMoreComplex() {
        String[] testStrings = {
            "It is strangely hard",
            "to generate random strings",
            "when its a haiku."
        };
        
        StringPrintStream sps = StringPrintStream.create();
        sps.println(testStrings[0]);
        sps.println(testStrings[1]);
        sps.println(testStrings[2]);
        String testStr = testStrings[0] + "\n" + testStrings[1] + "\n" +testStrings[2] + "\n";
        assertEquals(testStr, sps.getOutput());
    }
}