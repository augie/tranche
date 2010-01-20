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

import java.util.HashSet;
import java.util.Set;
import org.tranche.hash.span.HashSpan;
import org.tranche.network.StatusTableRow;
import org.tranche.users.User;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class TestServerConfiguration {

    public static final byte DATA_SERVER_FLAG = 1;
    public static final byte ROUTING_SERVER_FLAG = 2;
    public static final byte FAILING_DATA_SERVER_FLAG = 3;
    public final double failingProbability;
    public static final double DEFAULT_FAILING_PROBABILITY = 0.05;
    public final int ostensiblePort;
    public final String ostensibleHost;
    public final int actualPort;
    public final String actualHost;
    public final boolean isCoreServer;
    public final boolean isOnline;
    public final boolean isSSL;
    public final Set<HashSpan> hashSpans;
    public final Set<User> users;
    private StatusTableRow row = null;
    public final byte serverFlag;

    private TestServerConfiguration(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL, byte flag, Set<HashSpan> hashSpans, Set<User> users) {
        this(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, flag, hashSpans, users, DEFAULT_FAILING_PROBABILITY);
    }
    
    private TestServerConfiguration(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL, byte flag, Set<HashSpan> hashSpans, Set<User> users, double failingProbability) {
        this.ostensibleHost = ostensibleHost;
        this.ostensiblePort = ostensiblePort;
        this.actualHost = actualHost;
        this.actualPort = actualPort;
        this.isCoreServer = isCoreServer;
        this.isOnline = isOnline;
        this.isSSL = isSSL;
        this.serverFlag = flag;
        this.hashSpans = hashSpans;
        this.users = users;
        this.failingProbability = failingProbability;
    }
    
    

    /**
     * <p>Create a configuration for a test server to use with TestNetwork.</p>
     * <p>Note the difference between 'ostensible' and 'actual':</p>
     * <ul>
     *   <li>Ostensible: The faux address. E.g., tranche://aardvark.org:443</li>
     *   <li>Actual: The address used for test. E.g., tranche://127.0.0.1:1500</li>
     * </ul>
     * <p>Note that SSL will be the same for both ostensible and actual URL. (E.g., if SSL flag set to true, both will be ssl+tranche://...)</p>
     * @param ostensiblePort The faux port
     * @param ostensibleHost The faux host
     * @param actualPort The port to which the server will really be bound. It's your responsibility to make sure doesn't conflict with other test servers!
     * @param actualHost The actual host from which the server will run. For now, this should be '127.0.0.1'.
     * @param isCoreServer True if you want the server to be a core server, false otherwise. Impacts the test!
     * @param isOnline True if you want the server to be online, false otherwise. Note that offline servers are not only not run, but their table entry states they are offline (useful for negative tests)
     * @param isSSL True if you want server to use secure socket, false otherwise.
     * @return
     */
    public static TestServerConfiguration generateForDataServer(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL) {
        return new TestServerConfiguration(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, DATA_SERVER_FLAG, new HashSet<HashSpan>(), new HashSet<User>());
    }

    /**
     * <p>Create a configuration for a test server to use with TestNetwork.</p>
     * <p>Note the difference between 'ostensible' and 'actual':</p>
     * <ul>
     *   <li>Ostensible: The faux address. E.g., tranche://aardvark.org:443</li>
     *   <li>Actual: The address used for test. E.g., tranche://127.0.0.1:1500</li>
     * </ul>
     * <p>Note that SSL will be the same for both ostensible and actual URL. (E.g., if SSL flag set to true, both will be ssl+tranche://...)</p>
     * @param ostensiblePort The faux port
     * @param ostensibleHost The faux host
     * @param actualPort The port to which the server will really be bound. It's your responsibility to make sure doesn't conflict with other test servers!
     * @param actualHost The actual host from which the server will run. For now, this should be '127.0.0.1'.
     * @param isCoreServer True if you want the server to be a core server, false otherwise. Impacts the test!
     * @param isOnline True if you want the server to be online, false otherwise. Note that offline servers are not only not run, but their table entry states they are offline (useful for negative tests)
     * @param isSSL True if you want server to use secure socket, false otherwise.
     * @param hashSpans Hash spans to be used as the hash spans and target hash spans for the server.
     * @return
     */
    public static TestServerConfiguration generateForDataServer(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL, Set<HashSpan> hashSpans) {
        return new TestServerConfiguration(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, DATA_SERVER_FLAG, hashSpans, new HashSet<User>());
    }

    /**
     * <p>Create a configuration for a test server to use with TestNetwork.</p>
     * <p>Note the difference between 'ostensible' and 'actual':</p>
     * <ul>
     *   <li>Ostensible: The faux address. E.g., tranche://aardvark.org:443</li>
     *   <li>Actual: The address used for test. E.g., tranche://127.0.0.1:1500</li>
     * </ul>
     * <p>Note that SSL will be the same for both ostensible and actual URL. (E.g., if SSL flag set to true, both will be ssl+tranche://...)</p>
     * @param ostensiblePort The faux port
     * @param ostensibleHost The faux host
     * @param actualPort The port to which the server will really be bound. It's your responsibility to make sure doesn't conflict with other test servers!
     * @param actualHost The actual host from which the server will run. For now, this should be '127.0.0.1'.
     * @param isCoreServer True if you want the server to be a core server, false otherwise. Impacts the test!
     * @param isOnline True if you want the server to be online, false otherwise. Note that offline servers are not only not run, but their table entry states they are offline (useful for negative tests)
     * @param isSSL True if you want server to use secure socket, false otherwise.
     * @param hashSpans Hash spans to be used as the hash spans and target hash spans for the server.
     * @param users Users to be added to the server.
     * @return
     */
    public static TestServerConfiguration generateForDataServer(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL, Set<HashSpan> hashSpans, Set<User> users) {
        return new TestServerConfiguration(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, DATA_SERVER_FLAG, hashSpans, users);
    }
    
    /**
     * <p>Create a configuration for a failing test server to use with TestNetwork.</p>
     * <p>Note the difference between 'ostensible' and 'actual':</p>
     * <ul>
     *   <li>Ostensible: The faux address. E.g., tranche://aardvark.org:443</li>
     *   <li>Actual: The address used for test. E.g., tranche://127.0.0.1:1500</li>
     * </ul>
     * <p>Note that SSL will be the same for both ostensible and actual URL. (E.g., if SSL flag set to true, both will be ssl+tranche://...)</p>
     * @param ostensiblePort The faux port
     * @param ostensibleHost The faux host
     * @param actualPort The port to which the server will really be bound. It's your responsibility to make sure doesn't conflict with other test servers!
     * @param actualHost The actual host from which the server will run. For now, this should be '127.0.0.1'.
     * @param isCoreServer True if you want the server to be a core server, false otherwise. Impacts the test!
     * @param isOnline True if you want the server to be online, false otherwise. Note that offline servers are not only not run, but their table entry states they are offline (useful for negative tests)
     * @param isSSL True if you want server to use secure socket, false otherwise.
     * @param failingRate A value between 0.0 and 1.0 (inclusive) that is the probability the server will fail for any given operation
     * @return
     */
    public static TestServerConfiguration generateForFailingDataServer(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL, double failingRate) {
        return new TestServerConfiguration(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, FAILING_DATA_SERVER_FLAG, new HashSet<HashSpan>(), new HashSet<User>(), failingRate);
    }

    /**
     * <p>Create a configuration for a failing test server to use with TestNetwork.</p>
     * <p>Note the difference between 'ostensible' and 'actual':</p>
     * <ul>
     *   <li>Ostensible: The faux address. E.g., tranche://aardvark.org:443</li>
     *   <li>Actual: The address used for test. E.g., tranche://127.0.0.1:1500</li>
     * </ul>
     * <p>Note that SSL will be the same for both ostensible and actual URL. (E.g., if SSL flag set to true, both will be ssl+tranche://...)</p>
     * @param ostensiblePort The faux port
     * @param ostensibleHost The faux host
     * @param actualPort The port to which the server will really be bound. It's your responsibility to make sure doesn't conflict with other test servers!
     * @param actualHost The actual host from which the server will run. For now, this should be '127.0.0.1'.
     * @param isCoreServer True if you want the server to be a core server, false otherwise. Impacts the test!
     * @param isOnline True if you want the server to be online, false otherwise. Note that offline servers are not only not run, but their table entry states they are offline (useful for negative tests)
     * @param isSSL True if you want server to use secure socket, false otherwise.
     * @param hashSpans Hash spans to be used as the hash spans and target hash spans for the server.
     * @param failingRate A value between 0.0 and 1.0 (inclusive) that is the probability the server will fail for any given operation
     * @return
     */
    public static TestServerConfiguration generateForFailingDataServer(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL, Set<HashSpan> hashSpans, double failingRate) {
        return new TestServerConfiguration(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, FAILING_DATA_SERVER_FLAG, hashSpans, new HashSet<User>(), failingRate);
    }

    /**
     * <p>Create a configuration for a failing test server to use with TestNetwork.</p>
     * <p>Note the difference between 'ostensible' and 'actual':</p>
     * <ul>
     *   <li>Ostensible: The faux address. E.g., tranche://aardvark.org:443</li>
     *   <li>Actual: The address used for test. E.g., tranche://127.0.0.1:1500</li>
     * </ul>
     * <p>Note that SSL will be the same for both ostensible and actual URL. (E.g., if SSL flag set to true, both will be ssl+tranche://...)</p>
     * @param ostensiblePort The faux port
     * @param ostensibleHost The faux host
     * @param actualPort The port to which the server will really be bound. It's your responsibility to make sure doesn't conflict with other test servers!
     * @param actualHost The actual host from which the server will run. For now, this should be '127.0.0.1'.
     * @param isCoreServer True if you want the server to be a core server, false otherwise. Impacts the test!
     * @param isOnline True if you want the server to be online, false otherwise. Note that offline servers are not only not run, but their table entry states they are offline (useful for negative tests)
     * @param isSSL True if you want server to use secure socket, false otherwise.
     * @param hashSpans Hash spans to be used as the hash spans and target hash spans for the server.
     * @param users Users to be added to the server.
     * @param failingRate A value between 0.0 and 1.0 (inclusive) that is the probability the server will fail for any given operation
     * @return
     */
    public static TestServerConfiguration generateForFailingDataServer(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL, Set<HashSpan> hashSpans, Set<User> users, double failingRate) {
        return new TestServerConfiguration(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, FAILING_DATA_SERVER_FLAG, hashSpans, users, failingRate);
    }

    /**
     * <p>Create a configuration for a test server to use with TestNetwork.</p>
     * <p>Note the difference between 'ostensible' and 'actual':</p>
     * <ul>
     *   <li>Ostensible: The faux address. E.g., tranche://aardvark.org:443</li>
     *   <li>Actual: The address used for test. E.g., tranche://127.0.0.1:1500</li>
     * </ul>
     * <p>Note that SSL will be the same for both ostensible and actual URL. (E.g., if SSL flag set to true, both will be ssl+tranche://...)</p>
     * @param ostensiblePort The faux port
     * @param ostensibleHost The faux host
     * @param actualPort The port to which the server will really be bound. It's your responsibility to make sure doesn't conflict with other test servers!
     * @param actualHost The actual host from which the server will run. For now, this should be '127.0.0.1'.
     * @param isCoreServer True if you want the server to be a core server, false otherwise. Impacts the test!
     * @param isOnline True if you want the server to be online, false otherwise. Note that offline servers are not only not run, but their table entry states they are offline (useful for negative tests)
     * @param isSSL True if you want server to use secure socket, false otherwise.
     * @return
     */
    public static TestServerConfiguration generateForRoutingServer(int ostensiblePort, String ostensibleHost, int actualPort, String actualHost, boolean isCoreServer, boolean isOnline, boolean isSSL) {
        return new TestServerConfiguration(ostensiblePort, ostensibleHost, actualPort, actualHost, isCoreServer, isOnline, isSSL, ROUTING_SERVER_FLAG, new HashSet<HashSpan>(), new HashSet<User>());
    }

    /**
     * <p>Returns the StatusTableRow object associated with this server.</p>
     * @return
     */
    public synchronized StatusTableRow getStatusTableRow() {
        if (row == null) {
            row = new StatusTableRow(ostensibleHost, ostensiblePort, isSSL, isOnline);
        }
        return row;
    }

    /**
     * <p>Just a convenience. Generate URL using actual host and port (as opposed to ostensible).</p>
     * @return
     */
    public String getActualURL() {
        return IOUtil.createURL(actualHost, actualPort, isSSL);
    }

    /**
     * <p>Just a convenience. Generate URL using ostensible host and port (as opposed to actual).</p>
     * @return
     */
    public String getOstensibleURL() {
        return IOUtil.createURL(ostensibleHost, ostensiblePort, isSSL);
    }
}
