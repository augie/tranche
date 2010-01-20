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
package org.tranche.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JProgressBar;

/**
 * A very simple indeterminate progress bar.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class IndeterminateProgressBar extends GenericFrame {

    private String msg;
    JProgressBar progress;

    /**
     * Create an indeterminate progress bar. The msg will be painted in the title
     * and on the progress bar. The progress bar is positioned relative to the GUI.
     * @param msg The title/message you'd like to display 
     */
    public IndeterminateProgressBar(String msg) {
        super(msg);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());

        this.msg = msg;
        setSize(new Dimension(450, 100));

        progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setString(this.msg);
        progress.setStringPainted(true);
        add(progress, BorderLayout.CENTER);
    }

    public void start() {
        setVisible(true);
    }

    public void stop() {
        setVisible(false);
        dispose();
    }

    public void setDisposeAllowable(boolean userCanDispose) {
        if (!userCanDispose) {
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        } else {
            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }
    }
}
