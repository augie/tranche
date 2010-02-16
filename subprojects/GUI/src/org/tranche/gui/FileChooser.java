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

import java.awt.Component;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import javax.swing.JFileChooser;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class FileChooser extends JFileChooser {

    private String dialogTitle = null;
    private boolean multiSelectionEnabled = false;
    private File startSelectedFile = null;
    private File startSelectedDirectory = null;
    private int fileSelectionMode = FILES_AND_DIRECTORIES;
    private GenericFileFilter fileFilter = null;
    private ArrayBlockingQueue<String> abq = new ArrayBlockingQueue<String>(1);

    @Override
    public void setDialogTitle(String title) {
        dialogTitle = title;
    }

    @Override
    public void setMultiSelectionEnabled(boolean multiSelectionEnabled) {
        this.multiSelectionEnabled = multiSelectionEnabled;
    }

    @Override
    public void setFileSelectionMode(int mode) {
        this.fileSelectionMode = mode;
    }

    @Override
    public void setSelectedFile(File selectedFile) {
        this.startSelectedFile = selectedFile;
    }

    @Override
    public void setCurrentDirectory(File currentDirectory) {
        this.startSelectedDirectory = currentDirectory;

    }

    public void setFileFilter(GenericFileFilter filter) {
        fileFilter = filter;
    }

    @Override
    public int showDialog(Component relativeTo, String title) {
        return showOpenDialog(relativeTo);
    }

    @Override
    public int showOpenDialog(Component relativeTo) {
        // fall back on JFileChooser
        JFileChooser jFileChooser = makeJFileChooser();

        // get the option
        int option = jFileChooser.showOpenDialog(relativeTo);
        // only get the file if the user pressed ok/open
        if (option == JFileChooser.APPROVE_OPTION) {
            // for null pointer exceptions
            try {
                abq.offer(jFileChooser.getSelectedFile().getAbsolutePath());
                return APPROVE_OPTION;
            } catch (Exception e) {
                abq.offer("");
                return ERROR_OPTION;
            }
        } else {
            abq.offer("");
            return CANCEL_OPTION;
        }
    }

    /**
     * Helper method to make a JFileChooser and initialize it with all of the appropriate information.
     */
    private JFileChooser makeJFileChooser() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setMultiSelectionEnabled(this.multiSelectionEnabled);
        if (fileFilter != null) {
            jFileChooser.setFileFilter(fileFilter);
        }
        if (startSelectedDirectory != null) {
            jFileChooser.setCurrentDirectory(this.startSelectedDirectory);
        }
        if (startSelectedFile != null) {
            jFileChooser.setSelectedFile(startSelectedFile);
        }
        if (this.dialogTitle != null) {
            jFileChooser.setDialogTitle(dialogTitle);
        }
        jFileChooser.setFileSelectionMode(this.fileSelectionMode);

        return jFileChooser;
    }

    @Override
    public int showSaveDialog(Component relativeTo) {
        // always fall back on JFileChooser
        JFileChooser jFileChooser = makeJFileChooser();
        int option = jFileChooser.showSaveDialog(relativeTo);
        // only get the file if the user pressed ok/open
        if (option == JFileChooser.APPROVE_OPTION) {
            // for null pointer exceptions
            try {
                abq.offer(jFileChooser.getSelectedFile().getAbsolutePath());
                return APPROVE_OPTION;
            } catch (Exception e) {
                abq.offer("");
                return ERROR_OPTION;
            }
        } else {
            abq.offer("");
            return CANCEL_OPTION;
        }
    }

    @Override
    public File getSelectedFile() {
        try {
            String selectedFile = abq.take();
            if (selectedFile.equals("")) {
                return null;
            } else {
                return new File(selectedFile);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
