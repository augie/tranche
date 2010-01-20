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

import java.io.OutputStream;
import java.io.PrintStream;
import org.tranche.time.TimeUtil;

/**
 *
 * @author besmit
 */
public class TextTest extends TrancheTestCase {

    public void testNewline() {
        assertEquals("Expecting appropriate newline", Text.getNewLine(), System.getProperty("line.separator"));
    }

    public void testNewlineTokenizer() {
        final String TEXT = "Mary had a little lamb" + Text.getNewLine() + "it's fleece was white as snow" + Text.getNewLine() + "And everywhre that Mary went...";

        String TOKEN_TEXT = Text.tokenizeNewlines(TEXT);
        assertFalse("Shouldn't contain any newlines", TOKEN_TEXT.contains(System.getProperty("line.separator")));

        String DETOKENIZED_TEXT = Text.detokenizeNewlines(TOKEN_TEXT);
        assertEquals("Tokenizing new lines shouldn't be lossy!", TEXT, DETOKENIZED_TEXT);

        String NULL_TOKEN_TEXT = Text.tokenizeNewlines(null);
        assertNull("", NULL_TOKEN_TEXT);

        String NULL_DETOKENIZED_TEXT = Text.detokenizeNewlines(NULL_TOKEN_TEXT);
        assertNull("", NULL_DETOKENIZED_TEXT);
    }

    public void testGetPrettyEllapsedTimeString() {
        String a = Text.getPrettyEllapsedTimeString(3);
        assertEquals("", "3 milliseconds", a);
    }

    public void testPrettyTimeEllapsed() {

        long oneDay = 1000 * 60 * 60 * 24,
                oneHour = 1000 * 60 * 60,
                oneMinute = 1000 * 60,
                oneSecond = 1000,
                oneMillisecond = 1;

        String oneDayStr = Text.getPrettyEllapsedTimeString(oneDay);
        String oneHourStr = Text.getPrettyEllapsedTimeString(oneHour);
        String oneMinuteStr = Text.getPrettyEllapsedTimeString(oneMinute);
        String oneSecondStr = Text.getPrettyEllapsedTimeString(oneSecond);
        String oneMillisecondStr = Text.getPrettyEllapsedTimeString(oneMillisecond);
        String oneEachStr = Text.getPrettyEllapsedTimeString(oneDay + oneHour + oneMinute + oneSecond + oneMillisecond);
        String doubleEachStr = Text.getPrettyEllapsedTimeString(2 * (oneDay + oneHour + oneSecond + oneMillisecond));

        // Format may change... just print string
        System.out.println("One day test: " + oneDayStr);
        System.out.println("One hour test: " + oneHourStr);
        System.out.println("One minute test: " + oneMinuteStr);
        System.out.println("One second test: " + oneSecondStr);
        System.out.println("One millisecond test: " + oneMillisecondStr);
        System.out.println("Total test: " + oneEachStr);
        System.out.println("Double total test: " + doubleEachStr);

        // Should also handle doubles (since math returns doubles freq.)
        String oneDayStrDbl = Text.getPrettyEllapsedTimeString(oneDay);
        String oneHourStrDbl = Text.getPrettyEllapsedTimeString(oneHour);
        String oneMinuteStrDbl = Text.getPrettyEllapsedTimeString(oneMinute);
        String oneSecondStrDbl = Text.getPrettyEllapsedTimeString(oneSecond);
        String oneMillisecondStrDbl = Text.getPrettyEllapsedTimeString(oneMillisecond);
        String oneEachStrDbl = Text.getPrettyEllapsedTimeString(oneDay + oneHour + oneMinute + oneSecond + oneMillisecond);
        String doubleEachStrDbl = Text.getPrettyEllapsedTimeString(2 * (oneDay + oneHour + oneSecond + oneMillisecond));

        assertEquals("", oneDayStr, oneDayStrDbl);
        assertEquals("", oneHourStr, oneHourStrDbl);
        assertEquals("", oneMinuteStr, oneMinuteStrDbl);
        assertEquals("", oneSecondStr, oneSecondStrDbl);
        assertEquals("", oneMillisecondStr, oneMillisecondStrDbl);
        assertEquals("", oneEachStr, oneEachStrDbl);
        assertEquals("", doubleEachStr, doubleEachStrDbl);
    }

    public void testNullPrintStream() {
        PrintStream nps = Text.getNullPrintStream();
        nps.print((Object) "nothing");
        nps.print("nothing");
        nps.print(false);
        nps.print('n');
        nps.print(new char[0]);
        nps.print((double) 0.0);
        nps.print((float) 0.0);
        nps.print(0);
        nps.print((long) 0);
        nps.println((Object) "nothing");
        nps.println("nothing");
        nps.println(false);
        nps.println('n');
        nps.println(new char[0]);
        nps.println((double) 0.0);
        nps.println((float) 0.0);
        nps.println(0);
        nps.println((long) 0);
    }

    public void testNullOutputStream() throws Exception {
        OutputStream nos = Text.getNullOutputStream();
        nos.write(new byte[0]);
        nos.write(0);
        nos.write(new byte[0], 0, 0);
        nos.flush();
    }

    public void testGetFormattedBytes() {
        // 0B
        System.out.println(Text.getFormattedBytes(0));

        // <1KB
        System.out.println(Text.getFormattedBytes(1023));

        // 5TB
        System.out.println(Text.getFormattedBytes(5 * 1024 * 1024 * 1024 * 1024));

        // >1PB
        System.out.println(Text.getFormattedBytes(1024 * 1024 * 1024 * 1024 * 1024 + 512));
    }

    public void testGetWeekdayAndHour() {

        long now = TimeUtil.getTrancheTimestamp();
        final long DAY = 1000 * 60 * 60 * 24;

        // Today
        System.out.println(Text.getWeekdayAndHour(now));

        // Yesterday
        System.out.println(Text.getWeekdayAndHour(now - DAY));

        // -2
        System.out.println(Text.getWeekdayAndHour(now - 2 * DAY));

        // -3
        System.out.println(Text.getWeekdayAndHour(now - 3 * DAY));

        // -4
        System.out.println(Text.getWeekdayAndHour(now - 4 * DAY));

        // -5
        System.out.println(Text.getWeekdayAndHour(now - 5 * DAY));

        // -6
        System.out.println(Text.getWeekdayAndHour(now - 6 * DAY));

        // Week ago today
        System.out.println(Text.getWeekdayAndHour(now - 7 * DAY));
    }

    public void testGetFormattedDate() {
        long now = TimeUtil.getTrancheTimestamp();
        long MONTH = 1000 * 60 * 60 * 24 * 30;

        // Now
        System.out.println(Text.getFormattedDate(now - 0 * MONTH));

        // -1 month
        System.out.println(Text.getFormattedDate(now - 1 * MONTH));

        // -2 months
        System.out.println(Text.getFormattedDate(now - 2 * MONTH));

        // -3 months
        System.out.println(Text.getFormattedDate(now - 3 * MONTH));

        // -4 months
        System.out.println(Text.getFormattedDate(now - 4 * MONTH));

        // -5 months
        System.out.println(Text.getFormattedDate(now - 5 * MONTH));

        // -6 months
        System.out.println(Text.getFormattedDate(now - 6 * MONTH));

        // -7 months
        System.out.println(Text.getFormattedDate(now - 7 * MONTH));

        // -8 months
        System.out.println(Text.getFormattedDate(now - 8 * MONTH));

        // -9 months
        System.out.println(Text.getFormattedDate(now - 9 * MONTH));

        // -10 months
        System.out.println(Text.getFormattedDate(now - 10 * MONTH));

        // -11 months
        System.out.println(Text.getFormattedDate(now - 11 * MONTH));

        // Year ago (roughly)
        System.out.println(Text.getFormattedDate(now - 12 * MONTH));

        // -10 months
        System.out.println(Text.getFormattedDate(now - 13 * MONTH));

        // -11 months
        System.out.println(Text.getFormattedDate(now - 14 * MONTH));

        // Year ago (roughly)
        System.out.println(Text.getFormattedDate(now - 15 * MONTH));
        // Year ago (roughly)
        System.out.println(Text.getFormattedDate(now - 16 * MONTH));

        // -10 months
        System.out.println(Text.getFormattedDate(now - 17 * MONTH));

        // -11 months
        System.out.println(Text.getFormattedDate(now - 18 * MONTH));

    }

    public void testCurrentTime() throws Exception {
        // Cheap human-viewable test for formatted date now
        System.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
        Thread.sleep(1);
        System.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
        Thread.sleep(1);
        System.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
        Thread.sleep(1);
        System.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
        Thread.sleep(10);
        System.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
        Thread.sleep(10);
        System.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
        Thread.sleep(100);
        System.out.println(Text.getFormattedDate(TimeUtil.getTrancheTimestamp()));
    }

    public void testCurrentTimeTest() throws Exception {
        System.out.println(Text.getFormattedDateSimple(TimeUtil.getTrancheTimestamp()));
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testLevenshteinDistancesEqualUnweighted() throws Exception {
        final String str1 = "Bryan sells sea shells by the sea shore.";
        final String str1Dup = "Bryan sells sea shells by the sea shore.";
        final String str2 = "Mark sells sea shells by the sea shore.";
        final String str3 = "Augie sells sea shells by the sea shore.";

        long distance1 = Text.getLevenshteinDistance(str1, str1Dup);
        assertEquals("Expecting distance of 0 for equal strings.", 0, distance1);

        long distance2 = Text.getLevenshteinDistance(str1, str2);
        assertNotSame("Expecting distance not 0 for unequal strings.", (long) 0, distance2);

        long distance3 = Text.getLevenshteinDistance(str1, str3);
        assertNotSame("Expecting distance not 0 for unequal strings.", (long) 0, distance3);
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testLevenshteinDistancesUnequalUnweighted() throws Exception {
        final String str1 = "That is a witty kitty.";
        final String str2 = "That is not a witty kitty.";
        final String str3 = "That is not a kitty witty.";
        final String str4 = "Dr. Andrews sells sea shells by the sea shore.";

        long closest = Text.getLevenshteinDistance(str1, str2);
        long middle = Text.getLevenshteinDistance(str1, str3);
        long furthest = Text.getLevenshteinDistance(str1, str4);

        assertTrue("Asserting closest match.", closest < middle && closest < furthest);
        assertTrue("Asserting middle match.", middle > closest && middle < furthest);
        assertTrue("Asserting furthest match.", furthest > closest && furthest > middle);
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetCharacterDifferenceBetweenStrings() throws Exception {

        // Equal
        String str1 = "abcdefghijklmnop";
        String str2 = "abcdefghijklmnop";

        assertEquals("Should yield same count.", 0, Text.getCharacterDifferenceBetweenStrings(str1, str2));

        // Different
        str1 = "abcdefghijklmnop";
        str1 = "abcdefgabcqrs";

        assertEquals("Expecting a difference of 6: chars a,b,c,q,r,s plus three chars at end of first string", 9, Text.getCharacterDifferenceBetweenStrings(str1, str2));
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetCharacterCountDifferenceBetweenBase64Strings() throws Exception {
        // Equal
        String str1 = "01234456789=+/ppp==";
        String str2 = "9876544=+/ppp3210==";
        assertEquals("Should return no difference despite re-ordering", 0, Text.getCharacterCountDifferenceBetweenBase64Strings(str1, str2));

        // Different
        str1 = "0123456++//abcde==";
        str2 = "abcde++//abcde==";
        assertEquals("Should have found difference of 12", 12, Text.getCharacterCountDifferenceBetweenBase64Strings(str1, str2));

        // Exception
        str1 = "0123456789.";
        str2 = "0123456789=";
        try {
            int found = Text.getCharacterCountDifferenceBetweenBase64Strings(str1, str2);
            fail("Should have thrown an exception, instead found " + found + ".");
        } catch (Exception expected) { /* nope */ }
    }

    /**
     * 
     * @throws java.lang.Exception
     */
    public void testGetCharacterCountDifferenceBetweenBase16Strings() throws Exception {
        // Equal
        String str1 = "abcdef0123456789";
        String str2 = "abcdef0123456789";
        assertEquals("Should return difference of 0 despite reordering.", 0, Text.getCharacterCountDifferenceBetweenBase16Strings(str1, str2));

        // Different
        str1 = "abcdefabcdef";
        str2 = "abcdef0123456789";
        assertEquals("Should have found difference of 16", 16, Text.getCharacterCountDifferenceBetweenBase16Strings(str1, str2));

        // Exception
        str1 = "abcdefg";
        str2 = "abcdef0";

        try {
            int found = Text.getCharacterCountDifferenceBetweenBase16Strings(str1, str2);
            fail("Should have thrown an exception, instead found " + found + ".");
        } catch (Exception expected) { /* nope */ }
    }
}
