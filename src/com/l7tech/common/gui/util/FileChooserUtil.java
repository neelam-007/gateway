package com.l7tech.common.gui.util;

import javax.swing.*;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlException;
import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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
}
