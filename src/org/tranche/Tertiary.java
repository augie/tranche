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
package org.tranche;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class Tertiary extends Object {

    public static final String STRING_DONT_CARE = "%";
    public static final String STRING_FALSE = "0";
    public static final String STRING_TRUE = "1";
    public static final Tertiary TRUE = new Tertiary(STRING_TRUE);
    public static final Tertiary FALSE = new Tertiary(STRING_FALSE);
    public static final Tertiary DONT_CARE = new Tertiary(STRING_DONT_CARE);
    private final String value;

    public Tertiary(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public boolean equals(Tertiary tertiary) {
        return value.equals(tertiary.toString());
    }

    public static Tertiary valueOf(boolean value) {
        if (value) {
            return TRUE;
        } else {
            return FALSE;
        }
    }

    public static Tertiary valueOf(String value) {
        if (value.equals(STRING_TRUE)) {
            return TRUE;
        } else if (value.equals(STRING_FALSE)) {
            return FALSE;
        } else if (value.equals(STRING_DONT_CARE)) {
            return DONT_CARE;
        }
        return null;
    }

    public static Tertiary valueOf(short value) {
        if (value == 1) {
            return TRUE;
        } else if (value == 0) {
            return FALSE;
        } else if (value == 2) {
            return DONT_CARE;
        }
        return null;
    }

    public static Tertiary valueOf(int value) {
        if (value == 1) {
            return TRUE;
        } else if (value == 0) {
            return FALSE;
        } else if (value == 2) {
            return DONT_CARE;
        }
        return null;
    }

    public static Tertiary valueOf(long value) {
        if (value == 1) {
            return TRUE;
        } else if (value == 0) {
            return FALSE;
        } else if (value == 2) {
            return DONT_CARE;
        }
        return null;
    }
}
