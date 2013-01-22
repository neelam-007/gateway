package com.l7tech.gui.util;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for creating JFileChooser instances in a variety of situations.
 */
public class FileChooserUtil {
    protected static final Logger logger = Logger.getLogger(FileChooserUtil.class.getName());

    /**
     * Create a JFileChooser instance that has the selection listener
     * already added to it.
     *
     * @return a new JFileChooser instance.  Never null.
     * @throws AccessControlException if a new JFileChooser could not be created due to an access control exception
     */
    public static JFileChooser createJFileChooser() throws AccessControlException {
        return doWithJFileChooser(null);
    }

    /**
     * Used to keep track of the last directory that the user visited with a JFileChooser.
     */
    private static class JFileChooserSelectionListener implements ActionListener {
        public static File CURRENT_DIRECTORY = null;

        public void actionPerformed(ActionEvent e) {
            CURRENT_DIRECTORY = ((JFileChooser)e.getSource()).getCurrentDirectory();
        }
    }
    private static JFileChooserSelectionListener FILE_CHOOSER_SELECTION_LISTENER = new JFileChooserSelectionListener();

    /**
     * Returns the directory that shown in the last JFileChooser to be closed.
     * @return The directory to start the JFileChooser at
     */
    public static File getStartingDirectory() {
        return JFileChooserSelectionListener.CURRENT_DIRECTORY;
    }

    /**
     * Adds a listener to the JFileChooser to remember the last directory that was
     * shown.
     * @param fileChooser The JFileChooser to add the listener to
     */
    public static void addListenerToFileChooser(JFileChooser fileChooser) {
        fileChooser.addActionListener(FILE_CHOOSER_SELECTION_LISTENER);
    }
    
    /**
     * A factory that creates a JFileChooser with the selection listener
     * already added to it.
     *
     * @return a new JFileChooser instance.  Never null.
     */
    private static JFileChooser createJFileChooserHavePrivs() {
        JFileChooser fc = new JFileChooser(JFileChooserSelectionListener.CURRENT_DIRECTORY);
        fc.addActionListener(FILE_CHOOSER_SELECTION_LISTENER);
        return fc;
    }

    /**
     * Try to do something with a JFileChooser, failing with a graceful error message if running as an untrusted applet.
     *
     * @param fcu  the code that will be used by the JFileChooser, or null if you intend to use the returned
     *             JFileChooser yourself.
     *             Will not be invoked if no JFileChooser can be created.
     * @return the JFileChooser that was created.  Never null.
     * @throws AccessControlException if a new JFileChooser could not be created due to an access control exception
     */
    public static JFileChooser doWithJFileChooser(final FileChooserUser fcu) throws AccessControlException {
        return AccessController.doPrivileged(new PrivilegedAction<JFileChooser>() {
            public JFileChooser run() {
                try {
                    // Assume we are not running as an untrusted applet if we can read the current directory.
                    SecurityManager sm = System.getSecurityManager();
                    if ( sm != null ) {
                        sm.checkRead(new File("").getAbsolutePath());
                    }
                    
                    JFileChooser fc = createJFileChooserHavePrivs();
                    if (fcu != null) fcu.useFileChooser(fc);
                    return fc;
                } catch (SecurityException se) {
                    throw (AccessControlException)new AccessControlException("Unable to read current working directory").initCause(se);
                }
            }
        });
    }

    /** Interface implemented by callers of {@link FileChooserUtil#doWithJFileChooser}. */
    public interface FileChooserUser {
        void useFileChooser(JFileChooser fc);
    }

    /**
     * A helper class to create a file filter given a file extension and a file type description.
     * @param extension: a file extension such as .p10 or .pem
     * @param description: a file-type description.
     * @return a FileFilter.
     */
    public static FileFilter buildFilter(final String extension, final String description) {
        return new FileFilter() {
            public boolean accept(File f) {
                return  f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
            }
            public String getDescription() {
                return description;
            }
        };
    }

    /**
     * Display a file selector that will open a single file, with a single optional FileFilter type.
     * The selector dialog will be displayed
     * immediately and this method will not return until either the file has been opened, the user has canceled the
     * operation, or an error message has been shown to the user.
     *
     * @param parent  parent component for dialogs.  Required.
     * @param dialogTitle  title of dialog, eg "Open .DOC File".  Required.
     * @param fileFilter   FileFilter.  Optional.  See {@link #buildFilter(String, String)}.
     * @param defaultExtension  extension to add if user does not provide one, eg ".doc".  Optional.
     * @param loader  a UnaryThrows that will consume the contents of the file.  Required.
     * @return true if the file was saved successfully; false if the operation was canceled by the user or if there was a problem saving the file
     * (in which case an error message dialog has already been shown to the user)
     */
    public static boolean loadSingleFile(final Component parent, final String dialogTitle, final FileFilter fileFilter, final String defaultExtension, final Functions.UnaryThrows<Boolean, FileInputStream, IOException> loader) {
        final boolean[] loaded = { false };
        doWithJFileChooser(new FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                fc.setDialogTitle(dialogTitle);
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                fc.setMultiSelectionEnabled(false);
                if (fileFilter != null)
                    fc.setFileFilter(fileFilter);

                int result = fc.showOpenDialog(parent);
                if (JFileChooser.APPROVE_OPTION != result)
                    return;

                File file = fc.getSelectedFile();
                if (file == null)
                    return;

                if (defaultExtension != null && !file.getName().endsWith(defaultExtension)){
                    file = new File(file.toString() + defaultExtension);
                } else {
                    file = new File(file.getParent(), file.getName());
                }

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    loaded[0] = loader.call(fis);
                } catch (IOException e) {
                    final String msg = "Unable to open file: " + ExceptionUtils.getMessage(e);
                    logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                    DialogDisplayer.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
                } finally {
                    ResourceUtils.closeQuietly(fis);
                }
            }
        });
        return loaded[0];
    }

    /**
     * Display a file selector that will save a single file, with a single optional FileFilter type, displaying
     * a confirmation dialog if an existing file is about to be overwritten.  The selector dialog will be displayed
     * immediately and this method will not return until either the file has been saved, the user has canceled the
     * operation, or an error message has been shown to the user. 
     *
     * @param parent  parent component for dialogs.  Required.
     * @param dialogTitle  title of dialog, eg "Save .DOC File".  Required.
     * @param fileFilter   FileFilter.  Optional.  See {@link #buildFilter(String, String)}.
     * @param defaultExtension  extension to add if user does not provide one, eg ".doc".  Optional.
     * @param saver  a Saver that will write content to the file.  Required.  See {@link com.l7tech.util.FileUtils.ByteSaver}.
     * @return true if the file was saved successfully; false if the operation was canceled by the user or if there was a problem saving the file
     * (in which case an error message dialog has already been shown to the user)
     */
    public static boolean saveSingleFileWithOverwriteConfirmation(final Component parent, final String dialogTitle, final FileFilter fileFilter, final String defaultExtension, final FileUtils.Saver saver) {
        final boolean[] saved = { false };
        doWithJFileChooser(new FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                fc.setDialogTitle(dialogTitle);
                fc.setDialogType(JFileChooser.SAVE_DIALOG);
                fc.setMultiSelectionEnabled(false);
                if (fileFilter != null)
                    fc.setFileFilter(fileFilter);

                int result = fc.showSaveDialog(parent);
                if (JFileChooser.APPROVE_OPTION != result)
                    return;

                File file = fc.getSelectedFile();
                if (file == null)
                    return;

                //if user did not append .pem extension, we'll append to it
                if (defaultExtension != null && !file.getName().endsWith(defaultExtension)){
                    file = new File(file.toString() + defaultExtension);
                } else {
                    file = new File(file.getParent(), file.getName());
                }

                try {
                    //if file already exists, we need to ask for confirmation to overwrite.
                    if (file.exists()) {
                        result = JOptionPane.showOptionDialog(fc, "The file '" + file.getName() + "' already exists.  Overwrite?",
                                "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                        if (result != JOptionPane.YES_OPTION)
                            return;
                    }

                    FileUtils.save(file, saver);
                    saved[0] = true;
                } catch (IOException e) {
                    final String msg = "Unable to save file: " + ExceptionUtils.getMessage(e);
                    logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                    DialogDisplayer.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
                }
            }
        });
        return saved[0];
    }
}
