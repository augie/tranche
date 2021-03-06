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
package org.tranche.routing;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public interface RoutingTrancheServerListener {

    /**
     * <p>Fired when a RoutingTrancheServer instance adds one or more data server.</p>
     * @param hosts
     */
    public void dataServersAdded(String[] hosts);

    /**
     * <p>Fired when a RoutingTrancheServer instance removes one or more data server.</p>
     * @param hosts
     */
    public void dataServersRemoved(String[] hosts);
}
