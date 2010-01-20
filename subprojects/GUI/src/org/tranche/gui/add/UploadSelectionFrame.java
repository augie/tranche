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
package org.tranche.gui.add;

import org.tranche.project.ProjectSummary;
import org.tranche.gui.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import javax.swing.JComboBox;
import org.tranche.gui.project.ProjectPool;
import org.tranche.project.ProjectSummaryCache;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class UploadSelectionFrame extends GenericFrame {

    private boolean buttonPressed = false;
    private JComboBox comboBox = new JComboBox(new String[0]);
    private ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1);
    private static LinkedList<String> alphabetizedProjectList = new LinkedList<String>(),  alphabetizedHashList = new LinkedList<String>();    // start a thread that adds all the project cache items to the list


    static {
        Thread itemLoadingThread = new Thread() {

            public void run() {
                ProjectPool.waitForStartup();
                Collection<ProjectSummary> projects = ProjectPool.getProjectSummaries();

                for (ProjectSummary pfs : projects) {
                    try {
                        if (pfs.title != null && !pfs.title.trim().equals("") && !pfs.title.equals("?")) {
                            int index = 0;
                            synchronized (alphabetizedProjectList) {
                                for (String value : alphabetizedProjectList) {
                                    if (pfs.title.toLowerCase().compareTo(value.toLowerCase()) < 0) {
                                        break;
                                    }
                                    index++;
                                }
                                alphabetizedProjectList.add(index, pfs.title);
                                alphabetizedHashList.add(index, pfs.hash.toString());
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        };
        itemLoadingThread.setDaemon(true);
        itemLoadingThread.start();
    }

    public UploadSelectionFrame() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Select");
        setPreferredSize(new Dimension(500, 90));
        setSize(getPreferredSize());
        setLayout(new BorderLayout());

        // listen for the window being closed
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        if (!buttonPressed) {
                            queue.offer("");
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });

        comboBox.setFont(Styles.FONT_11PT_BOLD);
        synchronized (alphabetizedProjectList) {
            for (String title : alphabetizedProjectList) {
                comboBox.addItem(title);
            }
        }
        add(comboBox, BorderLayout.CENTER);

        GenericButton selectButton = new GenericButton("Select");
        selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        buttonPressed = true;
                        synchronized (alphabetizedProjectList) {
                            try {
                                queue.offer(alphabetizedHashList.get(alphabetizedProjectList.indexOf((String) comboBox.getSelectedItem())));
                            } catch (Exception ee) {
                            }
                        }
                        dispose();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        add(selectButton, BorderLayout.SOUTH);
    }

    public String getHash() throws Exception {
        return queue.take();
    }
}
