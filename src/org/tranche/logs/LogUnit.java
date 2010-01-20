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
package org.tranche.logs;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A single unit of the entire logging scheme that autoamitaclly adds timestamps
 * when appending messages to the log.
 * @author Jayson Falkner - jfalkner@umich.edu
 */
public class LogUnit {

    int log;
    StringBuffer buffer = new StringBuffer();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd kk:mm:ss:SSS");

    /**
     * Create a new log unit with log identifier and title of the log.
     * @param log
     * @param title
     */
    public LogUnit(int log, String title) {
        this.log = log;
        if (!SimpleLog.IS_DISABLED) {
            buffer.append("LU; TS:" + getTimestamp() + "; TITLE:" + title + "\n");
        }
    }

    /**
     * Append message to log unit.
     * @param message
     */
    public void log(String message) {
        if (!SimpleLog.IS_DISABLED) {
            buffer.append("  TS:" + getTimestamp() + "; MSG:" + message + "\n");
        }
    }

    /**
     * Retrieve entire log unit as a string.
     * @return
     */
    public String getMessage() {
        if (!SimpleLog.IS_DISABLED) {
            buffer.append("END; TS:" + getTimestamp() + "\n");
        }
        return buffer.toString();
    }

    private final String getTimestamp() {
        return sdf.format(new Date());
    }
}
