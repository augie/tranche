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
package org.tranche.network;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.hash.span.HashSpan;
import org.tranche.util.DevUtil;
import org.tranche.commons.RandomUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class NetworkRandomUtil {

    /**
     * <p>Creates a random StatusTableRow for tests.</p>
     * @return
     * @throws java.lang.Exception
     */
    public static StatusTableRow createRandomStatusTableRow() throws Exception {
        StatusTableRow row = new StatusTableRow(RandomUtil.getString(10));
        row.setName(RandomUtil.getString(10));
        row.setGroup(RandomUtil.getString(10));
        row.setPort(RandomUtil.getInt(1500));
        row.setIsOnline(RandomUtil.getBoolean());
        row.setIsSSL(RandomUtil.getBoolean());
        row.setIsDataStore(RandomUtil.getBoolean());
        row.setIsReadable(RandomUtil.getBoolean());
        row.setIsWritable(RandomUtil.getBoolean());
        row.setHashSpans(DevUtil.createRandomHashSpanSet(10));
        row.setTargetHashSpans(DevUtil.createRandomHashSpanSet(10));
        return row;
    }

    /**
     *
     * @param maxSize
     * @return
     * @throws java.lang.Exception
     */
    public static Set<StatusTableRow> createRandomStatusTableRowSet(int maxSize) throws Exception {
        Set<StatusTableRow> rows = new HashSet<StatusTableRow>();
        for (int i = 0; i < RandomUtil.getInt(maxSize); i++) {
            rows.add(NetworkRandomUtil.createRandomStatusTableRow());
        }
        return rows;
    }
}
