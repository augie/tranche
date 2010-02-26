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
package org.tranche.gui.util;

import org.tranche.gui.user.UserZipFileEvent;
import org.tranche.gui.user.UserZipFileListener;
import org.tranche.gui.project.replication.ProjectReplicationToolGUI;
import org.tranche.gui.advanced.AdvancedGUI;
import org.tranche.gui.meta.MetaDataEditFrame;
import org.tranche.util.PreferencesUtil;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.tranche.exceptions.CouldNotFindMetaDataException;
import org.tranche.exceptions.PassphraseRequiredException;
import org.tranche.flatfile.FlatFileTrancheServer;
import org.tranche.gui.ErrorFrame;
import org.tranche.gui.FileChooser;
import org.tranche.gui.GenericOptionPane;
import org.tranche.gui.GenericPopupFrame;
import org.tranche.gui.InformationPanel;
import org.tranche.gui.PassphraseFrame;
import org.tranche.gui.PreferencesFrame;
import org.tranche.gui.add.wizard.AddFileToolWizard;
import org.tranche.gui.get.wizard.GetFileToolWizard;
import org.tranche.gui.project.ProjectToolGUI;
import org.tranche.meta.MetaDataCache;
import org.tranche.hash.Base16;
import org.tranche.hash.Base64;
import org.tranche.hash.BigHash;
import org.tranche.meta.MetaData;
import org.tranche.project.ProjectSummary;
import org.tranche.security.SecurityUtil;
import org.tranche.security.WrongPassphraseException;
import org.tranche.time.TimeUtil;
import org.tranche.util.OperatingSystem;
import org.tranche.util.Text;
import org.tranche.users.UserZipFile;
import org.tranche.util.IOUtil;
import org.tranche.util.RandomUtil;

/**
 * Helpful static methods to be used in the Tranche GUI.
 * @author James "Augie" Hill - augman85@gmail.com
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class GUIUtil {

    private static AdvancedGUI AdvancedGUI = null;
    private static PreferencesFrame preferencesFrame = new PreferencesFrame();
    private static UserZipFile userZipFile = null;
    private static String userEmail = null;
    private static final List<UserZipFileListener> userZipFileListeners = new ArrayList<UserZipFileListener>();
    // for formatting numbers with commas
    public static NumberFormat integerFormat = NumberFormat.getInstance(), floatFormat = NumberFormat.getInstance();

    static {
        integerFormat.setGroupingUsed(true);
        integerFormat.setParseIntegerOnly(true);
        floatFormat.setGroupingUsed(true);
        floatFormat.setMaximumFractionDigits(1);
        floatFormat.setMinimumFractionDigits(1);
        floatFormat.setParseIntegerOnly(false);
    }
    // the following are standardized error messages
    public static final Exception USER_NOT_LOADED_EXCEPTION = new Exception("User file needs to be loaded.");
    public static final Exception PASSPHRASE_REQD_EXCEPTION = new Exception("You must enter a passphrase.");
    public static final Exception WRONG_PASSPHRASE_EXCEPTION = new Exception("Incorrect passphrase entered.");
    public static final Exception CANT_READ_CERT_EXCEPTION = new Exception("Can't read public certificate. Please make sure it is valid.");
    public static final Exception CANT_READ_KEY_EXCEPTION = new Exception("Can't read user's private key. Please make sure it is valid.");
    public static final Exception CANT_CONNECT_EXCEPTION = new Exception("Can't connect to any of the specified servers.");
    public static final Exception USER_CANT_WRITE_EXCEPTION = new Exception("Can't find any servers that you have permission to upload to.");
    public static final String NEWLINE = Text.getNewLine();
    private static ProjectReplicationToolGUI replicationTool = null;
    private static OperatingSystem userOS = null;

    private GUIUtil() {
    }

    public static void centerOnScreen(JFrame frame) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int) (d.getWidth() / 2 - frame.getWidth() / 2), (int) (d.getHeight() / 2 - frame.getHeight() / 2));
    }

    public static boolean isPassphraseRequiredException(Exception e) {
        boolean isAppropriateRuntimeException = e.getClass().equals(RuntimeException.class) || e.getMessage().equals("Can't decrypt the file! No passphrase specified.");
        boolean isPassphraseRequiredException = e.getClass().equals(PassphraseRequiredException.class);

        return isAppropriateRuntimeException || isPassphraseRequiredException;
    }

    public static RuntimeException getInvalidHashException(String hash) {
        return new RuntimeException("The hash is " + hash.length() + " characters and must be a string encoded in either Base16 (152 characters) or Base64 (105 characters). Hash: " + hash);
    }

    /**
     *This static method will return the directory in which the gui state files are to be saved. If the directory does not exist, it will be created.
     */
    public static String getGUIDirectory() {
        File directory = new File(FlatFileTrancheServer.getDefaultHomeDir(), "gui");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory.toString();
    }
    private static boolean isDisplayUrlMessage = false;

    /**
     * Display a file in the system browser.  If you want to display a
     * file, you must include the absolute path name.
     *
     * @param url the file's url (the url must start with either "http://" or "file://").
     */
    static public void displayURL(String url) {

        if (!isDisplayUrlMessage) {
            isDisplayUrlMessage = true;
            GenericOptionPane.showMessageDialog(null, "Some actions will open in the browser. Check your application bar below to see whether a browser or new page has been opened.\n\nEven though many actions open up a page in a browser, this message will only be shown once.", "Opening page in browser", JOptionPane.INFORMATION_MESSAGE);
        }

        // make sure the URL is safe
        url = GUIUtil.createSafeURL(url);

        String osName = null;
        try {
            osName = System.getProperty("os.name");

            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (osName.startsWith("Windows")) {
                // if the url starts with "file://", make sure it actually starts with "file:///"
                if (url.toLowerCase().startsWith("file://") && !url.toLowerCase().startsWith("file:///")) {
                    url.replaceFirst("file://", "file:///");
                }
                Process process = Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                int returnVal = process.waitFor();
                if (returnVal != 0) {
                    if (GUIUtil.getAdvancedGUI() != null) {
                        GUIUtil.getAdvancedGUI().ef.show(new Exception("Could not open file."), GUIUtil.getAdvancedGUI());
                    } else {
                        ErrorFrame ef = new ErrorFrame();
                        ef.set(new Exception("Could not open file."));
                        ef.show();
                    }
                }
            } else { //assume Unix or Linux
                String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++) {
                    if (Runtime.getRuntime().exec(new String[]{"which", browsers[count]}).waitFor() == 0) {
                        browser = browsers[count];
                    }
                }
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                } else {
                    Process process = Runtime.getRuntime().exec(new String[]{browser, url});
                    int returnVal = process.waitFor();
                    if (returnVal != 0) {
                        if (GUIUtil.getAdvancedGUI() != null) {
                            GUIUtil.getAdvancedGUI().ef.show(new Exception("Could not open file."), GUIUtil.getAdvancedGUI());
                        } else {
                            ErrorFrame ef = new ErrorFrame();
                            ef.set(new Exception("Could not open file."));
                            ef.show();
                        }
                    }
                }
            }

        } catch (Exception e) {
//            GenericOptionPane.showMessageDialog(e, errMsg);
            System.err.println(e.getClass().getSimpleName() + " occurred while opening URL in browser: " + e.getMessage());
            e.printStackTrace(System.err);
            ErrorFrame errorframe = new ErrorFrame();
            errorframe.show(e, GUIUtil.AdvancedGUI);
        }
    }

    public static UserZipFile promptForUserFile(JFileChooser jfc, Component relativeTo) throws Exception {
        return promptForUserFile(jfc, relativeTo, true);
    }

    public static UserZipFile promptForUserFile(JFileChooser jfc, Component relativeTo, boolean requireValid) throws Exception {
        // pop up a file chooser
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // make the last seleected user file the default
        try {
            File lastUserFile = new File(PreferencesUtil.get(PreferencesUtil.PREF_USER_FILE_LOCATION));
            if (lastUserFile.exists()) {
                jfc.setSelectedFile(lastUserFile);
            }
        } catch (Exception ee) {
        }

        // show the dialog
        int returnButton = jfc.showDialog(relativeTo, "Select User File (*.zip.encrypted)");

        // if nothing is selected, skip
        File selected = jfc.getSelectedFile();
        if (selected == null || returnButton == JFileChooser.CANCEL_OPTION) {
            return null;
        }

        // set the file
        final UserZipFile uzf = new UserZipFile(selected);

        // if it is encrypted, ask for passphrase
        final PassphraseFrame pf = new PassphraseFrame();
        pf.setLocationRelativeTo(relativeTo);
        pf.setVisible(true);

        // just in case the passphrase is blank
        try {
            SecurityUtil.decryptInMemory(pf.getCurrentPassphrase(), IOUtil.getBytes(uzf.getFile()));
            pf.setPassphraseCorrect(true);
        } catch (Exception ex) {
            pf.setPassphraseCorrect(false);
        }

        // add a key listener
        pf.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                try {
                    SecurityUtil.decryptInMemory(pf.getCurrentPassphrase(), IOUtil.getBytes(uzf.getFile()));
                    pf.setPassphraseCorrect(true);
                } catch (Exception ex) {
                    pf.setPassphraseCorrect(false);
                }
            }
        });

        // catch password errors and show a error dialog
        uzf.setPassphrase(pf.getPassphrase());

        // check if they loaded a valid file
        if (uzf.getCertificate() == null) {
            throw new RuntimeException("You must select a valid user file!");
        }

        // save the user file location
        try {
            PreferencesUtil.set(PreferencesUtil.PREF_USER_FILE_LOCATION, selected.getAbsolutePath());
            PreferencesUtil.save();
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        // check the date of the certificate
        if (requireValid) {
            try {
                uzf.getCertificate().checkValidity(new Date(TimeUtil.getTrancheTimestamp()));
            } catch (CertificateExpiredException cee) {
                throw new CertificateExpiredException("The user file you are using is expired. Please obtain a new one.");
            } catch (CertificateNotYetValidException cnyve) {
                // Show message about checking system time, offer option to report error.
                throw new CertificateNotYetValidException("That user file is not yet valid. Please check your system time to make sure it is not behind.\nIf your system time is correct, please report this as an error.");
            }
        }

        // return the zip file
        return uzf;
    }

    public static void showHash(BigHash hash, ClipboardOwner clipboardOwner, Component relativeTo) {
        int pressedButton = GenericOptionPane.showConfirmDialog(relativeTo, hash.toString() + "\nWould you like to copy this hash to your clipboard?", "Tranche Hash", JOptionPane.YES_NO_OPTION);
        if (pressedButton == JOptionPane.YES_OPTION) {
            copyToClipboard(hash.toString(), clipboardOwner);
        }
    }

    public static void showMoreInfo(ProjectSummary summary, Component relativeTo) {
        InformationPanel infoPanel = new InformationPanel();
        infoPanel.set("Hash", summary.hash.toString());
        infoPanel.set("Title", summary.title);
        infoPanel.set("Uploaded By", summary.uploader);
        infoPanel.set("Uploaded", Text.getFormattedDate(summary.uploadTimestamp));
        infoPanel.set("Size", GUIUtil.integerFormat.format(summary.files) + " files, " + Text.getFormattedBytes(summary.size));
        if (summary.oldVersion != null) {
            infoPanel.set("Old Version", summary.oldVersion.toString());
        }
        if (summary.newVersion != null) {
            infoPanel.set("Old Version", summary.newVersion.toString());
        }
        infoPanel.set("Encrypted?", String.valueOf(summary.isEncrypted));
        if (summary.isEncrypted) {
            infoPanel.set("Published?", String.valueOf(summary.isPublished));
        }
        infoPanel.set("Hidden?", String.valueOf(summary.isHidden));
        infoPanel.set("Description", summary.description);

        GenericPopupFrame frame = new GenericPopupFrame(summary.title, infoPanel, false);
        frame.setSize(500, 400);
        if (relativeTo == null) {
            centerOnScreen(frame);
        } else {
            frame.setLocationRelativeTo(relativeTo);
        }
        frame.setVisible(true);
    }

    /**
     *
     * @param hash
     * @param relativeTo
     */
    public static void openGetFileToolWizard(BigHash hash, String uploaderName, Long uploadTimestamp, String relativePath, Component relativeTo) {
        try {
            GetFileToolWizard gui = new GetFileToolWizard();
            if (relativeTo == null) {
                centerOnScreen(gui);
            } else {
                gui.setLocationRelativeTo(relativeTo);
            }
            gui.setVisible(true);
            if (hash != null) {
                gui.setHash(hash);
            }
            if (uploaderName != null && uploadTimestamp != null) {
                gui.selectUploader(uploaderName, uploadTimestamp, relativePath);
            }
        } catch (Exception e) {
            ErrorFrame ef = new ErrorFrame();
            ef.set(e);
            ef.show(relativeTo);
        }
    }

    public static void openAddFileToolWizard(Component relativeTo) {
        try {
            // create the upload gui
            AddFileToolWizard gui = new AddFileToolWizard();
            // if there is a user loaded, set the user in the upload gui
            if (getUser() != null) {
                gui.setUser(getUser());
            }
            // open in the default platform location and show frame
            gui.setLocationRelativeTo(relativeTo);
            gui.setVisible(true);
        } catch (Exception e) {
            ErrorFrame ef = new ErrorFrame();
            ef.set(e);
            ef.show(relativeTo);
        }
    }

    /**
     *
     * @param hash
     * @param relativeTo
     */
    public static void openProjectToolGUI(BigHash hash, Component relativeTo) {
        ProjectToolGUI gui = new ProjectToolGUI();
        try {
            if (relativeTo != null) {
                gui.setLocationRelativeTo(relativeTo);
            } else {
                centerOnScreen(gui);
            }
            gui.setVisible(true);
            if (hash != null) {
                gui.open(hash, null);
            }
        } catch (CouldNotFindMetaDataException e) {
            GenericOptionPane.showMessageDialog(gui, e.getMessage(), "Data Set Not Found", JOptionPane.ERROR_MESSAGE);
            gui.setVisible(false);
        } catch (WrongPassphraseException e) {
            GenericOptionPane.showMessageDialog(gui, e.getMessage(), "Wrong Passphrase", JOptionPane.ERROR_MESSAGE);
            gui.setVisible(false);
        } catch (Exception e) {
            GenericOptionPane.showMessageDialog(gui, "Could not download the project file.", "Data Set Not Found", JOptionPane.ERROR_MESSAGE);
            gui.setVisible(false);
        }
    }

    /**
     *
     * @param hash
     * @param relativeTo
     */
    public static void openMetaDataEditFrame(BigHash hash, Component relativeTo) {
        try {
            MetaDataEditFrame mdef = new MetaDataEditFrame();
            mdef.setLocationRelativeTo(relativeTo);
            mdef.setVisible(true);
            if (hash != null) {
                mdef.setMetaDataHash(hash);
            }
        } catch (Exception e) {
            ErrorFrame ef = new ErrorFrame();
            ef.set(e);
            ef.show(relativeTo);
        }
    }

    /**
     * <p>Given a root directory and a file, returns the top-most subdirectory containing that file. Useful for recursive deletes.</p>
     * @param root The top-most directory. We are looking to find a directory below this.
     * @param file The file which is in a subdirectory. This method recursively finds the topmost directory containing this file but in the paramater root directory.
     */
    public static File getTopmostSubdirectoryContaining(File root, File file) {

        // Whoops, make sure it's in a subdirectory
        if (!file.getAbsolutePath().contains(root.getAbsolutePath())) {
            return null;
        }

        if (file.getParentFile().equals(root)) {
            return file;
        }

        return getTopmostSubdirectoryContaining(root, file.getParentFile());
    }

    /**
     * <p>Attempts to gather character/file encoding, but guesses if fails.</p>
     * @return
     */
    private static String getCharacterEncoding() {
        // Get the encoding
        String encoding = null;

        try {
            encoding = System.getProperty("file.encoding");
        } catch (Exception nope) {
        }

        if (encoding == null) {
            try {
                encoding = new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding();
            } catch (Exception nope) {
            }
        }

        // If failed to find encoding, guess
        if (encoding == null) {
            encoding = "UTF-8";
            System.err.println("Failed to find encoding, guessing: " + encoding);
        }

        return encoding;
    }

    /**
     * Returns base 16, url-encoded hash
     */
    public static String createURLEncodeBase16Hash(BigHash hash) {

        // Hash must be Base16, url-encoded
        String encoded = Base16.encode(hash.toByteArray());

        try {
            encoded = URLEncoder.encode(encoded, getCharacterEncoding());
        } catch (UnsupportedEncodingException nope) {
        }

        return encoded;
    }

    /**
     * @param str A url-encoded string representing a hash.
     * @return BigHash
     */
    public static BigHash createBigHashFromURLEncodedString(String str) {
        try {
            return BigHash.createHashFromString(URLDecoder.decode(str, getCharacterEncoding()));
        } catch (UnsupportedEncodingException ex) {
        }

        return null;
    }

    /**
     * Returns base 64, url-encoded hash
     */
    public static String createURLEncodeBase64Hash(BigHash hash) {

        // Hash must be Base64 url-encoded
        String encoded = Base64.encodeBytes(hash.toByteArray());

        try {
            encoded = URLEncoder.encode(encoded, getCharacterEncoding());
        } catch (UnsupportedEncodingException ex) {
        }

        return encoded;
    }

    /**
     * URL-encodes a string.
     */
    public static String createURLEncodedString(String str) throws Exception {

        str = URLEncoder.encode(str, getCharacterEncoding());

        return str;
    }

    /**
     * Please add anything to this to help create safer urls.
     */
    public static String createSafeURL(String str) {
        return str.replaceAll(" ", "%20");
    }

    /**
     * <p>Prompts user until writeable directory selected.</p>
     */
    public static File userSelectDirectoryForWrite(String title, JFrame frame) {

        File tmpSelectedDir;
        JFileChooser jfc;

        while (true) {

            jfc = makeNewFileChooser();
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            jfc.setDialogTitle(title);

            int action = jfc.showSaveDialog(frame);

            // Choose file for peaklist...
            if (action == JFileChooser.APPROVE_OPTION) {
                tmpSelectedDir = jfc.getSelectedFile();
            } // User bails
            else {
                return null;
            }

            if (tmpSelectedDir.exists() && tmpSelectedDir.canWrite()) {
                break;
            } else if (!tmpSelectedDir.exists()) {
                GenericOptionPane.showMessageDialog(
                        frame,
                        "Directory not found.",
                        "The directory you selected no longer exists. Please select a different directory.",
                        JOptionPane.WARNING_MESSAGE);
            } else if (!tmpSelectedDir.canWrite()) {
                GenericOptionPane.showMessageDialog(
                        frame,
                        "Permissions error.",
                        "Cannot write to selected directory, insufficient priveledges. Please select a different directory.",
                        JOptionPane.WARNING_MESSAGE);
            }
        } // Continues until save directory selected

        return tmpSelectedDir;
    }

    /**
     * <p>Prompts user until writeable file selected.</p>
     */
    public static File userSelectFileForWrite(String title, JFrame frame) {

        File tmpSelectedFile;
        JFileChooser jfc;

        while (true) {

            jfc = makeNewFileChooser();
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setDialogTitle(title);
            jfc.setCurrentDirectory(new File(PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE)));
            int action = jfc.showSaveDialog(frame);

            // Choose file for peaklist...
            if (action == JFileChooser.APPROVE_OPTION) {
                tmpSelectedFile = jfc.getSelectedFile();
                break;
            } // User bails
            else {
                return null;
            }

        } // Continues until save directory selected

        return tmpSelectedFile;
    }

    /**
     * <p>Prompts user for file.</p>
     */
    public static File userSelectFileForRead(String title, JFrame frame) {

        File tmpSelectedFile;
        JFileChooser jfc;

        while (true) {

            jfc = makeNewFileChooser();
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setDialogTitle(title);
            jfc.setCurrentDirectory(new File(PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE)));
            int action = jfc.showOpenDialog(frame);

            // Choose file for peaklist...
            if (action == JFileChooser.APPROVE_OPTION) {
                tmpSelectedFile = jfc.getSelectedFile();
                break;
            } // User bails
            else {
                return null;
            }

        } // Continues until save directory selected

        return tmpSelectedFile;
    }

    public static AdvancedGUI getAdvancedGUI() {
        return AdvancedGUI;
    }

    public static void setAdvancedGUI(AdvancedGUI frame) {
        AdvancedGUI = frame;
    }

    public static UserZipFile getUser() {
        return userZipFile;
    }

    public static void setUser(UserZipFile _userZipFile) {
        UserZipFile oldUserZipFile = userZipFile;
        userZipFile = _userZipFile;
        if (userZipFile != null) {
            synchronized (userZipFileListeners) {
                for (UserZipFileListener listener : userZipFileListeners) {
                    listener.userSignedOut(new UserZipFileEvent(oldUserZipFile));
                    listener.userSignedIn(new UserZipFileEvent(userZipFile));
                }
            }
        } else {
            synchronized (userZipFileListeners) {
                for (UserZipFileListener listener : userZipFileListeners) {
                    listener.userSignedOut(new UserZipFileEvent(oldUserZipFile));
                }
            }
        }
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static void setUserEmail(String url) {
        if (url == null || url.trim().equals("")) {
            userEmail = null;
            return;
        }
        userEmail = url;
    }

    public static void addUserZipFileListener(UserZipFileListener listener) {
        synchronized (userZipFileListeners) {
            userZipFileListeners.add(listener);
        }
    }

    public static void openMetaData(BigHash hash, Component relativeTo) {
        try {
            // create and show the meta data edit frame
            MetaDataEditFrame mdef = new MetaDataEditFrame();

            // set the servers to all the known servers
            MetaData metaData = MetaDataCache.get(hash, true);
            mdef.setMetaData(metaData);

            // set the user
            if (GUIUtil.getAdvancedGUI() != null) {
                if (GUIUtil.getAdvancedGUI().getUser() != null) {
                    mdef.setUserZipFile(GUIUtil.getAdvancedGUI().getUser());
                }
            }

            // show the frame
            mdef.setLocationRelativeTo(relativeTo);
            mdef.setVisible(true);
        } catch (Exception e) {
            ErrorFrame ef = new ErrorFrame();
            ef.show(e, relativeTo);
        }
    }

    public static String generateRandomPassphrase(int length) {
        char[] chars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        String passphrase = "";
        for (int i = 0; i < length; i++) {
            passphrase = passphrase + chars[RandomUtil.getInt(chars.length)];
        }
        return passphrase;
    }

    public static void copyToClipboard(String string, ClipboardOwner owner) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), owner);
    }

    /**
     * Wraps any component in a panel with optional parameter to set an inset (padding). Simple way to pad components, e.g., icons.
     * @param insetTop The top inset value to use in pixels
     * @param insetLeft The left inset value to use in pixels
     * @param insetBottom The bottom inset value to use in pixels
     * @param insetRight The right inset value to use in pixels
     */
    public static JPanel wrapComponentInPanel(Component c, int insetTop, int insetLeft, int insetBottom, int insetRight) {
        JPanel componentWrapperPanel = new JPanel();

        componentWrapperPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(insetTop, insetLeft, insetBottom, insetRight);
        gbc.weightx = 1;
        gbc.weighty = 1;
        componentWrapperPanel.add(c, gbc);

        return componentWrapperPanel;
    }

    public static JFileChooser makeNewFileChooser() {
        try {
            return new FileChooser();
        } catch (NoClassDefFoundError e) {
            return new JFileChooser();
        }
    }

    /**
     * 
     * @return
     */
    public static PreferencesFrame getPreferencesFrame() {
        preferencesFrame.loadPreferences();
        return preferencesFrame;
    }
}
