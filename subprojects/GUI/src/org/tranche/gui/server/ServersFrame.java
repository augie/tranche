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
package org.tranche.gui.server;

import java.awt.BorderLayout;
import java.util.Collection;
import org.tranche.gui.GenericFrame;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServersFrame extends GenericFrame {

    private ServersPanel serversPanel;

    public ServersFrame(String title, Collection<String> serverHosts) {
        super(title);
        serversPanel = new ServersPanel(serverHosts);
        setTitle(title);
        setSize(500, 300);
        setLayout(new BorderLayout());
        add(serversPanel, BorderLayout.CENTER);
    }

    public ServersPanel getPanel() {
        return serversPanel;
    }
}
