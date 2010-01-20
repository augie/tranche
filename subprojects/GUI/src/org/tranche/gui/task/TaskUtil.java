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
package org.tranche.gui.task;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.tranche.gui.util.GUIUtil;
import org.tranche.util.PreferencesUtil;

/**
 * Utilities for task GUI.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class TaskUtil {

    /**
     * Helper method to choose a file
     */
    public static File getFile(String title, JFrame frame) {

        File tmpSelectedFile;
        JFileChooser jfc;

        while (true) {

            jfc = GUIUtil.makeNewFileChooser();
            jfc.setFileSelectionMode(jfc.FILES_ONLY);
            jfc.setDialogTitle(title);
            if (PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE) != null) {
                jfc.setCurrentDirectory(new File(PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE)));
            }
            int action = jfc.showSaveDialog(frame);

            // Choose file for peaklist...
            if (action == jfc.APPROVE_OPTION) {
                tmpSelectedFile = jfc.getSelectedFile();
                break;
            } // User bails
            else {
                return null;
            }

        } // Continues until save directory selected

        return tmpSelectedFile;

    } // getFile
}
