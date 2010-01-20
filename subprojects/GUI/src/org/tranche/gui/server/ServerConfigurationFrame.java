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

import org.tranche.gui.GenericPopupFrame;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class ServerConfigurationFrame extends GenericPopupFrame {

    public ServerConfigurationPanel serverConfigurationPanel;

    public ServerConfigurationFrame() {
        super("Server Configuration", new ServerConfigurationPanel(), false);
        serverConfigurationPanel = (ServerConfigurationPanel) super.getComponent();
        setSize(800, 450);
    }

    public ServerConfigurationPanel getPanel() {
        return serverConfigurationPanel;
    }
}
