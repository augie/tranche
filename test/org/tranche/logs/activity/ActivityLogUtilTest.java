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
package org.tranche.logs.activity;

import java.util.Random;
import org.tranche.security.Signature;
import org.tranche.logs.activity.ActivityLogEntry;
import org.tranche.logs.activity.SignatureIndexEntry;
import org.tranche.time.TimeUtil;
import org.tranche.util.DevUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ActivityLogUtilTest extends TrancheTestCase {

    // If more activities, just add to this array. 
    private final static byte[] actions = {
        Activity.DELETE_DATA,
        Activity.DELETE_META_DATA,
        Activity.SET_DATA,
        Activity.SET_META_DATA
    };

    /**
     * <p>Make sure all action being logged have unique values. Pretty simple.</p>
     */
    public void testActionBytesUnique() throws Exception {
        for (int index1 = 0; index1 < actions.length; index1++) {
            for (int index2 = 0; index2 < actions.length; index2++) {
                if (index1 == index2) {
                    continue;
                }
                byte b1 = actions[index1];
                byte b2 = actions[index2];
                assertNotSame("Bytes should be different!", b1, b2);
            }
        }

        // A couple quick assertion. 
        assertTrue("If this fails, check that the bytes in Activity for actions are correct.", Activity.isData(Activity.DELETE_DATA));
        assertFalse("If this fails, check that the bytes in Activity for actions are correct.", Activity.isMetaData(Activity.DELETE_DATA));
        assertTrue("If this fails, check that the bytes in Activity for actions are correct.", Activity.isData(Activity.SET_DATA));
        assertFalse("If this fails, check that the bytes in Activity for actions are correct.", Activity.isMetaData(Activity.SET_DATA));

        assertTrue("If this fails, check that the bytes in Activity for actions are correct.", Activity.isMetaData(Activity.DELETE_META_DATA));
        assertFalse("If this fails, check that the bytes in Activity for actions are correct.", Activity.isData(Activity.DELETE_META_DATA));
        assertTrue("If this fails, check that the bytes in Activity for actions are correct.", Activity.isMetaData(Activity.SET_META_DATA));
        assertFalse("If this fails, check that the bytes in Activity for actions are correct.", Activity.isData(Activity.SET_META_DATA));
    }

    public void testToAndFromActivityLogEntryByteArray() throws Exception {
        ActivityLogEntry activityLogEntry1 = getRandomActivityLogEntry();
        System.out.println("Log entry #1: " + activityLogEntry1);

        byte[] activityLogEntry1Bytes = ActivityLogUtil.toActivityLogByteArray(activityLogEntry1);
        ActivityLogEntry activityLogEntry1Verify = ActivityLogUtil.fromActivityLogByteArray(activityLogEntry1Bytes);

        assertEquals("Expectin two values to be the same.", activityLogEntry1, activityLogEntry1Verify);

        ActivityLogEntry activityLogEntry2 = getRandomActivityLogEntry();
        System.out.println("Log entry #2: " + activityLogEntry2);

        if (activityLogEntry1.equals(activityLogEntry2)) {
            fail("Against staggering odds, we randomly generated the same activity twice. Go buy a lottery ticket.");
        }

        byte[] activityLogEntry2Bytes = ActivityLogUtil.toActivityLogByteArray(activityLogEntry2);
        ActivityLogEntry activityLogEntry2Verify = ActivityLogUtil.fromActivityLogByteArray(activityLogEntry2Bytes);

        assertEquals("Expectin two values to be the same.", activityLogEntry2, activityLogEntry2Verify);

        assertFalse("Should not be equal", activityLogEntry1.equals(activityLogEntry2));
        assertFalse("Should not be equal", activityLogEntry1Verify.equals(activityLogEntry2));
        assertFalse("Should not be equal", activityLogEntry1.equals(activityLogEntry2Verify));
        assertFalse("Should not be equal", activityLogEntry1Verify.equals(activityLogEntry2Verify));
    }

    public void testToAndFromSignatureIndexEntryByteArray() throws Exception {
        SignatureIndexEntry entry1 = getRandomSignatureIndexEntry();
        System.out.println("Signature entry #1: " + entry1);

        byte[] entry1Bytes = ActivityLogUtil.toSignatureHeaderByteArray(entry1);
        SignatureIndexEntry entry1Verify = ActivityLogUtil.fromSignatureHeaderByteArray(entry1Bytes);

        assertEquals("Expecting two values to be the same.", entry1, entry1Verify);

        SignatureIndexEntry entry2 = getRandomSignatureIndexEntry();
        System.out.println("Signature entry #2: " + entry2);

        if (entry1.equals(entry2)) {
            fail("Entry 1 equals entry 2. The odds of this happening were approximately 1:20,000,000,000,000.");
        }

        byte[] entry2Bytes = ActivityLogUtil.toSignatureHeaderByteArray(entry2);
        SignatureIndexEntry entry2Verify = ActivityLogUtil.fromSignatureHeaderByteArray(entry2Bytes);

        assertEquals("Expecting two values to be the same.", entry1, entry1Verify);

        assertFalse("Should not be equal", entry1.equals(entry2));
        assertFalse("Should not be equal", entry1Verify.equals(entry2));
        assertFalse("Should not be equal", entry1.equals(entry2Verify));
        assertFalse("Should not be equal", entry1Verify.equals(entry2Verify));
    }

    public void testToAndFromSignatureByteArray() throws Exception {
        Signature sig = DevUtil.getBogusSignature();
        byte[] sigBytes = ActivityLogUtil.toSignatureByteArray(sig);
        Signature sigVerify = ActivityLogUtil.fromSignatureByteArray(sigBytes);
        assertEquals("Should verify", sig, sigVerify);
    }

    public static Activity getRandomActivity() throws Exception {
        return new Activity(getRandomActivityByte(), DevUtil.getBogusSignature(), DevUtil.getRandomBigHash());
    }

//    public static Activity getRandomActivity(boolean isPast) throws Exception {
//        return new Activity(getRandomTimestamp(isPast), getRandomActivityByte(), DevUtil.getBogusSignature(), DevUtil.getRandomBigHash());
//    }
    public static ActivityLogEntry getRandomActivityLogEntry() {
        return new ActivityLogEntry(getRandomTimestamp(), getRandomActivityByte(), getRandomSignatureIndex(), DevUtil.getRandomBigHash());
    }

    public static SignatureIndexEntry getRandomSignatureIndexEntry() {
        int index = getRandomSignatureIndex();
        long offset = getRandomSignatureOffset(index);
        int length = getRandomSignatureLength();
        return new SignatureIndexEntry(index, offset, length);
    }

    public static long getRandomTimestamp() {
        Random r = new Random();
        return getRandomTimestamp(r.nextBoolean());
    }

    public static long getRandomTimestamp(boolean isPast) {
        Random r = new Random();
        final int DAY = 1000 * 60 * 60 * 24;

        long timestamp = TimeUtil.getTrancheTimestamp();

        // Past
        if (isPast) {
            timestamp -= r.nextInt(DAY * 7);
        } // Future
        else {
            timestamp += r.nextInt(DAY * 7);
        }

        return timestamp;
    }

    public static byte getRandomActivityByte() {
        Random r = new Random();
        int index = r.nextInt(actions.length);
        return actions[index];
    }

    public static int getRandomSignatureIndex() {
        // Doesn't matter, but say we have 100K registered users
        int maxSignatureIndex = 100000;
        Random r = new Random();
        return r.nextInt(maxSignatureIndex);
    }
    final public static int avgSignatureSizeInBytes = 780;

    public static long getRandomSignatureOffset(int index) {
        Random r = new Random();

        long offset = avgSignatureSizeInBytes * index;

        // Simulate variability by adding or removing some bytes (+- 2 bytes/entry)
        if (r.nextBoolean()) {
            return offset - r.nextInt(index) * 2;
        } else {
            return offset + r.nextInt(index) * 2;
        }
    }

    public static int getRandomSignatureLength() {
        Random r = new Random();
        // Average size +- 10 bytes
        if (r.nextBoolean()) {
            return avgSignatureSizeInBytes - r.nextInt(10);
        } else {
            return avgSignatureSizeInBytes + r.nextInt(10);
        }
    }
}
