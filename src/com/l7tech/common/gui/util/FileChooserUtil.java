package com.l7tech.common.gui.util;

import javax.swing.*;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlException;
import java.io.File;

/**
 * Utilities for creating JFileChooser instances in a variety of situations.
 */
public class FileChooserUtil {
    protected static final Logger logger = Logger.getLogger(FileChooserUtil.class.getName());

    /**
     * Create a JFileChooser, supporting running as an applet and
     * working around Java bug parade #4711700.  Will retry
     * for up to one second.
     *
     * @return a new JFileChooser instance.  Never null.
     * @throws AccessControlException if a new JFileChooser could not be created due to an access control exception
     * @throws RuntimeException if a new JFileChooser could not be created
     */
    public static JFileChooser createJFileChooser() throws AccessControlException {
        return doWithJFileChooser(null);
    }

    /**
     * A factory that creates a JFileChooser, working around Java bug parade #4711700.  Will retry
     * for up to one second.  Assumes that caller has already invoked doPrivileged.
     *
     * @return a new JFileChooser instance.  Never null.
     * @throws RuntimeException if a new JFileChooser could not be created.
     */
    private static JFileChooser createJFileChooserHavePrivs() throws RuntimeException {
        JFileChooser fc = null;
        int tries = 40;
        while (fc == null) {
            try {
                fc = new JFileChooser();
                break;
            } catch (NullPointerException nfe) {
                // Bug parade 4711700 -- retry a few times before giving up
                if (--tries < 0)
                    throw new RuntimeException("J4711700 workaround: retry count exceeded", nfe);
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("J4711700 workaround: interrupted while waiting to retry", nfe);
                }
                logger.finest("J4711700 workaround: retrying");
            }
        }
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
     * @throws RuntimeException if a new JFileChooser could not be created
     */
    public static JFileChooser doWithJFileChooser(final FileChooserUser fcu) throws AccessControlException {
        return AccessController.doPrivileged(new PrivilegedAction<JFileChooser>() {
            public JFileChooser run() {
                try {
                    // Assume we are not running as an untrusted applet if we can read the current directory.
                    System.getSecurityManager().checkRead(new File("").getAbsolutePath());
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
