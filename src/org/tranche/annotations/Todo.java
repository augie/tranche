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
package org.tranche.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * Available only at compile-time, this type is used to include notes about
 * what might or should be done to the code base. Eventually such notes can
 * be collecte up using a JDK 5 APT module in to reports/or some kind of
 * "to do" list.
 * </p>
 *
 * @author Brian Maso
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Todo {

    /**
     * <p>A brief description of the todo item.</p>
     * @return
     */
    public String desc();

    /**
     * <p>Day of month. Start with 1 for the first (i.e., not zero-indexed). If unknown, use 0.</p>
     * @return
     */
    public int day();

    /**
     * <p>Month starting with 1 (January) and ending with 12 (December). If unknown, use 0.</p>
     * @return
     */
    public int month();

    /**
     * <p>E.g., 2002 for 2002 AD. If unknown, use 0.</p>
     * @return
     */
    public int year();

    /**
     * <p>The author of the annotation. If unknown, use "unknown".</p>
     * @return
     */
    public String author();
}
