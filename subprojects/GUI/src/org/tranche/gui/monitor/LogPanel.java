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

import java.awt.BorderLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JPanel;
import org.tranche.gui.GenericScrollPane;
import org.tranche.gui.GenericTextArea;
import org.tranche.gui.Styles;
import org.tranche.util.IOUtil;
import org.tranche.util.TempFileUtil;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class LogPanel extends JPanel {

    public static final int MAX_CHARACTERS_TO_DISPLAY = 15000;
    public File tempLogFile = TempFileUtil.createTemporaryFile();
    public FileOutputStream logFile;
    public GenericTextArea log = new GenericTextArea();

    public LogPanel() {
        setName("Log");
        setLayout(new BorderLayout());

        // modify the textarea
        log.setEditable(false);
        log.setWrapStyleWord(true);
        log.setFont(Styles.FONT_11PT);
        log.setMargin(new Insets(5, 5, 5, 5));

        // create and add the scroll pane containing the log
        GenericScrollPane scrollPane = new GenericScrollPane(log);
        scrollPane.setBackground(log.getBackground());
        scrollPane.setBorder(Styles.BORDER_NONE);
        add(scrollPane, BorderLayout.CENTER);

        // add temp log message
        addMessage("Log file located at " + tempLogFile.getAbsolutePath());
    }

    public synchronized void start() {
        if (logFile == null) {
            try {
                logFile = new FileOutputStream(tempLogFile.getAbsolutePath(), true);
            } catch (Exception e) {
                addMessage("Could not open a print stream to the log file.");
            }
        }
    }

    public synchronized void stop() {
        if (logFile != null) {
            try {
                logFile.flush();
            } catch (Exception e) {
            }
            try {
                logFile.close();
            } catch (Exception e) {
            }
            logFile = null;
        }
    }

    public synchronized void clean() {
        stop();
        IOUtil.safeDelete(tempLogFile);
    }

    public synchronized void addMessage(String message) {
        if (logFile != null) {
            try {
                logFile.write(String.valueOf(message + "\n").getBytes());
            } catch (Exception e) {
            }
        }
        String newText = log.getText() + "\n" + message;
        if (newText.length() > MAX_CHARACTERS_TO_DISPLAY) {
            newText = newText.substring(newText.length() - MAX_CHARACTERS_TO_DISPLAY);
        }
        log.setText(newText);
    }

    public synchronized void saveLogFileTo(File file) throws IOException {
        try {
            logFile.flush();
        } catch (Exception e) {
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        IOUtil.copyFile(tempLogFile, file);
    }
}
