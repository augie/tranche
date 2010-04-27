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
package org.tranche.gui.get;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericFrame;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.commons.ThreadUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class SelectUploaderFrame extends GenericFrame {

    private SelectUploaderPanel panel = new SelectUploaderPanel();

    public SelectUploaderFrame() {
        super("Select Uploader");
        setSize(new Dimension(400, 300));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        GenericButton button = new GenericButton("OK");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SelectUploaderFrame.this.dispose();
            }
        });
        add(button, BorderLayout.SOUTH);
    }

    public void setMetaData(BigHash hash, MetaData metaData) {
        panel.setMetaData(hash, metaData);
    }

    public void waitForSelection() {
        Thread t = new Thread() {

            @Override
            public void run() {
                while (isVisible()) {
                    ThreadUtil.sleep(250);
                }
            }
        };
        t.setDaemon(true);
        t.start();
        try {
            t.join();
        } catch (Exception e) {
        }
    }
}
