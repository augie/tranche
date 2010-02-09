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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.tranche.util.IOUtil;
import org.tranche.util.TestUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class StatusTableTest extends NetworkPackageTestCase {

    @Override()
    protected void setUp() throws Exception {
        super.setUp();
        StatusTable.setDebug(true);
        StatusTableRow.setDebug(true);
    }

    @Override()
    protected void tearDown() throws Exception {
        super.tearDown();
        StatusTable.setDebug(false);
        StatusTableRow.setDebug(false);
    }

    public void testCreate() throws Exception {
        TestUtil.printTitle("StatusTableTest:testCreate()");

        // rows
        Set<StatusTableRow> rows = NetworkRandomUtil.createRandomStatusTableRowSet(10);

        // create
        StatusTable st = new StatusTable();
        st.setRows(rows);

        // verify
        if (st == null) {
            fail("Object is null");
        }
        if (!StatusTableRow.areEqual(rows, st.getRows())) {
            fail("Status table row collections are inequal.");
        }
    }

    public void testRecreate() throws Exception {
        TestUtil.printTitle("StatusTableTest:testRecreate()");

        // rows
        Set<StatusTableRow> rows = NetworkRandomUtil.createRandomStatusTableRowSet(10);

        // create
        StatusTable st1 = new StatusTable();
        st1.setRows(rows);

        // recreate
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        st1.serialize(StatusTable.VERSION_LATEST, StatusTableRow.VERSION_LATEST, oos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        StatusTable st = new StatusTable(ois);

        // verify
        assertEquals(st1, st);
    }

    public void testSorted() throws Exception {
        TestUtil.printTitle("StatusTableTest:testSorted()");

        // rows
        Set<StatusTableRow> rows = NetworkRandomUtil.createRandomStatusTableRowSet(10);

        // get correct sort
        List<String> sortedHosts = new LinkedList<String>();
        for (StatusTableRow str : rows) {
            sortedHosts.add(str.getHost());
        }
        // perform sort
        Collections.sort(sortedHosts);

        // create
        StatusTable st = new StatusTable();
        st.setRows(rows);
        List<StatusTableRow> tableRows = st.getRows();

        // verify sort
        for (int i = 0; i < sortedHosts.size(); i++) {
            if (!tableRows.get(i).getHost().equals(sortedHosts.get(i))) {
                fail("Improper row order.");
            }
        }
    }

    public void testSetRow() throws Exception {
        TestUtil.printTitle("StatusTableTest:testSetRow()");

        // rows
        Set<StatusTableRow> rows = NetworkRandomUtil.createRandomStatusTableRowSet(10);

        // get correct sort
        List<String> sortedHosts = new LinkedList<String>();
        for (StatusTableRow str : rows) {
            sortedHosts.add(str.getHost());
        }

        // create
        StatusTable st = new StatusTable();
        st.setRows(rows);

        // new
        StatusTableRow str = NetworkRandomUtil.createRandomStatusTableRow();
        st.setRow(str);
        sortedHosts.add(str.getHost());

        // perform sort
        Collections.sort(sortedHosts);

        // verify
        assertEquals(rows.size() + 1, st.size());
        assertEquals(rows.size() + 1, st.getRows().size());
        assertEquals(rows.size() + 1, st.getHosts().size());
        assertEquals(rows.size() + 1, st.getURLs().size());
        // verify correct sort
        List<StatusTableRow> tableRows = st.getRows();
        for (int i = 0; i < sortedHosts.size(); i++) {
            if (!tableRows.get(i).getHost().equals(sortedHosts.get(i))) {
                fail("Improper row order.");
            }
        }
        List<String> tableHosts = st.getHosts();
        for (int i = 0; i < sortedHosts.size(); i++) {
            if (!tableHosts.get(i).equals(sortedHosts.get(i))) {
                fail("Improper host order.");
            }
        }
        List<String> tableURLs = st.getURLs();
        for (int i = 0; i < sortedHosts.size(); i++) {
            if (!IOUtil.parseHost(tableURLs.get(i)).equals(sortedHosts.get(i))) {
                fail("Improper URL order.");
            }
        }
    }

    public void testUpdateRow() throws Exception {
        TestUtil.printTitle("StatusTableTest:testUpdateRow()");

        StatusTable st = new StatusTable();
        StatusTableRow str = NetworkRandomUtil.createRandomStatusTableRow();
        st.setRow(str);

        // update
        StatusTableRow newRow = NetworkRandomUtil.createRandomStatusTableRow();
        newRow.setHost(str.getHost());
        st.setRow(newRow);

        // verify
        assertEquals(1, st.size());
        assertEquals(1, st.getRows().size());
        assertEquals(1, st.getHosts().size());
        assertEquals(1, st.getURLs().size());
        assertEquals(newRow, st.getRows().get(0));
        assertEquals(newRow.getHost(), st.getHosts().get(0));
        assertEquals(newRow.getURL(), st.getURLs().get(0));
    }

    public void testRemoveRow() throws Exception {
        TestUtil.printTitle("StatusTableTest:testRemoveRow()");

        // row
        StatusTableRow str = NetworkRandomUtil.createRandomStatusTableRow();

        // create
        StatusTable st = new StatusTable();
        st.setRow(str);

        // remove
        st.removeRow(str.getHost());

        // verify
        assertEquals(0, st.size());
        assertEquals(0, st.getHosts().size());
        assertEquals(0, st.getURLs().size());
        assertEquals(0, st.getRows().size());
    }

    public void testGetPortion() throws Exception {
        TestUtil.printTitle("StatusTableTest:testGetPortion()");

        // rows
        List<StatusTableRow> rows = new LinkedList<StatusTableRow>();
        for (int i = 0; i < 10; i++) {
            rows.add(NetworkRandomUtil.createRandomStatusTableRow());
        }

        // get sorted hosts
        List<String> sortedHosts = new LinkedList<String>();
        for (StatusTableRow str : rows) {
            sortedHosts.add(str.getHost());
        }
        // perform sort
        Collections.sort(sortedHosts);
        // determine second and second to last
        String firstHost = sortedHosts.get(0), secondHost = sortedHosts.get(1), secondToLastHost = sortedHosts.get(sortedHosts.size() - 2), thirdToLastHost = sortedHosts.get(sortedHosts.size() - 3);

        // create
        StatusTable st = new StatusTable();
        st.setRows(rows);

        // non-loop
        {
            // get
            List<StatusTableRow> rowsPortion = st.getRows(secondHost, secondToLastHost);
            // should be 9 returned
            assertEquals(7, rowsPortion.size());
            // verify first and last
            assertEquals(secondHost, rowsPortion.get(0).getHost());
            assertEquals(thirdToLastHost, rowsPortion.get(rowsPortion.size() - 1).getHost());
        }
        // loop
        {
            // get
            List<StatusTableRow> rowsPortion = st.getRows(secondToLastHost, secondHost);
            // should be 3 returned
            assertEquals(3, rowsPortion.size());
            // verify first and last
            assertEquals(secondToLastHost, rowsPortion.get(0).getHost());
            assertEquals(firstHost, rowsPortion.get(rowsPortion.size() - 1).getHost());
        }
    }
}
