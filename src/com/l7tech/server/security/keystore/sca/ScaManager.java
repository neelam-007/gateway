package com.l7tech.server.security.keystore.sca;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ProcResult;
import static com.l7tech.common.util.ProcUtils.args;
import static com.l7tech.common.util.ProcUtils.exec;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that takes care of the low level details of pulling out or installing an SCA6000 keystore on the current node.
 */
public class ScaManager {
    protected static final Logger logger = Logger.getLogger(ScaManager.class.getName());

    private static final String DEFAULT_TAR_PATH = "/bin/tar";
    private static final String DEFAULT_SCAKIOD_PATH = "/opt/sun/sca6000/bin/scakiod_load";
    private static final String DEFAULT_KEYDATA_DIR = "/var/opt/sun/sca6000/keydata";

    /** The system property that holds the path to the tar exectuable. */
    public static final String PROPERTY_TAR_PATH = "tarPath";

    /** The system property that holds the path to the scakiod_load exectuable. */
    public static final String PROPERTY_SCAKIOD_PATH = "scakiodPath";

    /** The system property that holds the path to the keydata directory. */
    public static final String PROPERTY_KEYDATA_DIR = "keydataDir";

    private static final Object globalMutex = new Object();

    private final File tar;
    private final File scakiodLoad;
    private final File keydataDir;


    public ScaManager() throws ScaException {
        tar = findFile("tar", PROPERTY_TAR_PATH, DEFAULT_TAR_PATH, true, false, true, false);
        scakiodLoad = findFile("scakiod_load", PROPERTY_SCAKIOD_PATH, DEFAULT_SCAKIOD_PATH, true, false, true, false);
        keydataDir = findFile("keydata directory", PROPERTY_KEYDATA_DIR, DEFAULT_KEYDATA_DIR, false, true, true, true);
    }

    private File findFile(String thing, String sysprop, String defaultValue,
                          boolean mustBeFile, boolean mustBeDirectory, boolean mustBeReadable, boolean mustBeWritable)
            throws ScaException
    {
        String path = System.getProperty(sysprop, defaultValue);
        if (path == null || path.length() < 1)
            throw new ScaException("Unable to find " + thing + ": System property " + sysprop + " is not valid");
        File file = new File(path);
        if (!file.exists())
            throw new ScaException("Unable to find " + thing + " at path: "
                                   + path + ".  Set system property " + sysprop + " to override.");
        if (mustBeFile && !file.isFile())
            throw new ScaException(thing + " at path: "
                                   + path + " is not a plain file.  Set system property " + sysprop + " to override.");
        if (mustBeDirectory && !file.isDirectory())
            throw new ScaException(thing + " at path: "
                                   + path + " is not a directory.  Set system property " + sysprop + " to override.");
        if (mustBeReadable && !file.canRead())
            throw new ScaException(thing + " at path: "
                                   + path + " is not readable by the Gateway process.  Set system property " + sysprop + " to override.");
        if (mustBeWritable && !file.canWrite())
            throw new ScaException(thing + " at path: "
                                   + path + " is not writable by the Gateway process.  Set system property " + sysprop + " to override.");
        return file;
    }

    /** @return local names of all files and directories immediately under the keydataDir that appear to contain keystore data. */
    private String[] findKeystoreFilenames() {
        File[] files = keydataDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.startsWith("sca.") && name.contains(".{");
            }
        });

        String[] ret = new String[files.length];
        for (int i = 0; i < files.length; i++)
            ret[i] = files[i].getName();
        return ret;
    }

    /**
     * Stop the scakiod on this node.  The scakiod must be stopped while keystore data is loaded or
     * updated.
     * @throws ScaException if there is a problem invoking the scakiod_load program
     */
    private void doStopSca() throws ScaException {
        try {
            ProcResult result = exec(null, scakiodLoad, args("stop"), null, true);
            int status = result.getExitStatus();
            switch (status) {
                case 0:
                    // Successful stop
                    return;
                case 1:
                    // Scakiod process wasn't running.  Log warning but treat as success for our purposes.
                    logger.warning("Went to stop scakiod, but it was not running");
                    return;
                default:
                    throw new ScaException("Unknown exit status " + status + " when shutting down scakiod");
            }
        } catch (IOException e) {
            throw new ScaException(e);
        }
    }

    /**
     * Start the scakiod on this node.  The scakiod must be running to allow crypto operations to run
     * if they use key information that is not already cached within the card.
     *
     * @throws ScaException if there is a problem invoking the scakiod_load program
     */
    private void doStartSca() throws ScaException {
        try {
            exec(null, scakiodLoad, args("start"), null);
        } catch (IOException e) {
            throw new ScaException(e);
        }
    }

    /**
     * Get the keystore data.  Caller is responsible for ensuring that the scakiod is stopped whenever this is
     * called.
     *
     * @return bytes of a gzipped tar file, whose contents are relative to the KEYDATA_DIR, containing
     *         all files and directories composing the keystore data currently on this node.
     * @throws ScaException if there is a problem invoking the tar program
     */
    private byte[] doLoadKeydata() throws ScaException {
        try {
            return exec(keydataDir, tar, args("czf", "-", findKeystoreFilenames()), null).getOutput();
        } catch (IOException e) {
            throw new ScaException(e);
        }
    }

    /**
     * Replace the existing key data with new key data.  Caller is responsible for ensuring that the
     * scakiod is stopped whenever this is called.
     *
     * @param data  bytes of a gzipped tar file, whose contents are relative to the KEYDATA_DIR, containing
     *              all files and directories composing the updated keystore data to use for this node.
     * @throws ScaException if there is a problem invoking the tar program
     */
    private void doSaveKeydata(byte[] data) throws ScaException {
        try {
            exec(keydataDir, tar, args("xzf", "-"), data);
        } catch (IOException e) {
            throw new ScaException(e);
        }
    }


    /**
     * Safely get all of the keystore data for this node as a gzipped tarfile, relative to the keydata
     * directory.
     * <p/>
     * The keystore data will only be usable on another node whose SCA6000 is initialized with exactly the
     * same master key.  Caller is responsible for ensuring that the intended recipient of this data will
     * be able to do use it.
     * <p/>
     * This method will shut down the scakiod while the data is being read and start it again afterwards.
     * Crypto operations that require key material not already cached on the board may fail while the
     * scakiod is not running.
     *
     * @return the bytes of a gzipped tar file, with all paths relative to the keydata directory, containing
     *         all keystore data from this node.
     * @throws ScaException if there is a problem gathering this data.
     */
    public byte[] loadKeydata() throws ScaException {
        synchronized (globalMutex) {
            try {
                doStopSca();
                return doLoadKeydata();
            } finally {
                try {
                    doStartSca();
                } catch (ScaException e) {
                    logger.log(Level.WARNING, "Unable to restart scakiod: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }

    /**
     * Safely replace the keystore data for this node with new data provided in the form of a gzipped tarfile,
     * relative to the keydata directory.
     * <p/>
     * This node will only be able to use keystore data that originated on another node whose SCA6000 is
     * initialized with exactly the same master key as this node.  Caller is responsible for ensuring that
     * the keystore data will be usable on this node.
     * <p/>
     * This method will shut down the scakiod while the data is being written and start it again afterwards.
     * Crypto operations that require key material not already cached on the board may fail while the
     * scakiod is not running.
     *
     * @param keydata  the bytes of a gzipped tar file, with all paths relative to the keydata directory,
     *                 containing all keystore data intended for this node.  The keystore data must match
     *                 the master key already installed on this node's SCA6000.
     * @throws ScaException if there is a problem storing this data.
     */
    public void saveKeydata(byte[] keydata) throws ScaException {
        synchronized (globalMutex) {
            try {
                doStopSca();
                doSaveKeydata(keydata);
            } finally {
                try {
                    doStartSca();
                } catch (ScaException e) {
                    logger.log(Level.WARNING, "Unable to restart scakiod: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }
}

