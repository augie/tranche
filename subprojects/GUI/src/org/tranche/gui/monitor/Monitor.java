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
package org.tranche.gui.monitor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.tranche.gui.GenericFrame;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public abstract class Monitor extends GenericFrame {

    public abstract void start();

    public abstract void stop();

    public void clean() {
        log.clean();
    }

    // listen for window closing to make sure this is cleaned up


    {
        addWindowListener(new WindowAdapter() {

            public void windowClosed(WindowEvent e) {
                stop();
                clean();
            }
        });
    }
    private boolean isListening = false;

    public boolean isListening() {
        return isListening;
    }

    public void setListening(boolean isListening) {
        this.isListening = isListening;
    }
    protected LogPanel log = new LogPanel();

    /**
     * <p>This overriding method will call stop() and clean() before disposing of the frame for memory and disk purposes.</p>
     */
    public void dispose() {
        stop();
        clean();
        super.dispose();
    }
}
