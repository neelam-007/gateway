package com.l7tech.server.security.keystore.sca;

import com.l7tech.common.util.*;
import static com.l7tech.common.util.ProcUtils.args;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that takes care of the low level details of pulling out or installing an SCA6000 keystore on the current node.
 */
public class ScaManager {
    protected static final Logger logger = Logger.getLogger(ScaManager.class.getName());

    private static final String DEFAULT_SCAKIOD_PATH = "/opt/sun/sca6000/bin/scakiod_load";
    private static final String DEFAULT_LOAD_KEYDATA_PATH = "/ssg/libexec/load_keydata";
    private static final String DEFAULT_SAVE_KEYDATA_PATH = "/ssg/libexec/save_keydata";
    private static final String DEFAULT_WIPE_KEYDATA_PATH = "/ssg/libexec/wipe_keydata";

    private static final String SYSPROP_BASE = ScaManager.class.getPackage().getName() + ".";

    public static final String PROPERTY_LOAD_KEYDATA_PATH = SYSPROP_BASE + "loadKeydataPath";
    public static final String PROPERTY_SAVE_KEYDATA_PATH = SYSPROP_BASE + "saveKeydataPath";
    public static final String PROPERTY_WIPE_KEYDATA_PATH = SYSPROP_BASE + "wipeKeydataPath";
    public static final String PROPERTY_SCAKIOD_PATH = SYSPROP_BASE + "scakiodPath";

    private static final Object globalMutex = new Object();

    private final File sudo;
    private final File scakiodLoad;
    private final File loadKeydata;
    private final File saveKeydata;
    private final File wipeKeydata;

    public ScaManager() throws ScaException {
        try {
            sudo = SudoUtils.findSudo();
            scakiodLoad = FileUtils.findConfiguredFile("scakiod_load", PROPERTY_SCAKIOD_PATH, DEFAULT_SCAKIOD_PATH, true, false, true, false, false);
            loadKeydata = FileUtils.findConfiguredFile("load_keydata.sh", PROPERTY_LOAD_KEYDATA_PATH, DEFAULT_LOAD_KEYDATA_PATH, true, false, false, false, false);
            saveKeydata = FileUtils.findConfiguredFile("save_keydata.sh", PROPERTY_SAVE_KEYDATA_PATH, DEFAULT_SAVE_KEYDATA_PATH, true, false, false, false, false);
            wipeKeydata = FileUtils.findConfiguredFile("wipe_keydata.sh", PROPERTY_WIPE_KEYDATA_PATH, DEFAULT_WIPE_KEYDATA_PATH, true, false, false, false, false);
        } catch (IOException e) {
            throw new ScaException(e.getMessage(), e);
        }
    }

    /**
     * Stop the scakiod on this node.  The scakiod must be stopped while keystore data is loaded or
     * updated.
     * @throws ScaException if there is a problem invoking the scakiod_load program
     */
    protected void doStopSca() throws ScaException {
        try {
            // Work-around for SCA STALL deadlock (Bug #3802) -- wait for any pending DB changes to be
            // written out before we stop the kiod
            Thread.sleep(2000L);
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
        } catch (InterruptedException e) {
            logger.warning("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Start the scakiod on this node.  The scakiod must be running to allow crypto operations to run
     * if they use key information that is not already cached within the card.
     *
     * @throws ScaException if there is a problem invoking the scakiod_load program
     */
    protected void doStartSca() throws ScaException {
        try {
            exec(null, scakiodLoad, args("start"), null, false);
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
     * @throws ScaException if there is a problem invoking the command, or if the command returned nonzero
     */
    private byte[] doLoadKeydata() throws ScaException {
        try {
            return exec(null, loadKeydata, args("reallyForSure"), null, false).getOutput();
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
     * @return a ProcResult if the save was successful.  Never null.
     * @throws ScaException if there is a problem invoking the command, or if the command returned nonzero
     *                      the keystore data may be corrupted in the latter case.
     */
    private ProcResult doSaveKeydata(byte[] data) throws ScaException {
        // TODO change so it is actually safe (writes to ".new" directory)
        try {
            return exec(null, saveKeydata, args("reallyForSure"), data, false);
        } catch (IOException e) {
            throw new ScaException(e);
        }
    }

    /**
     * Deletes everything from the keydata directory.  The configurator can invoke this after
     * zeroizing the keystore.  Nobody else should use this method, ever.
     *
     * @return a ProcResult if the wipe was successful.  Never null.
     * @throws ScaException if there is a problem invoking the command, or if the command returned nonzero
     *                      the keystore data may be corrupted in the latter case.
     */
    private ProcResult doWipeKeydata() throws ScaException {
        try {
            return exec(null, wipeKeydata, args("reallyForSure"), null, false);
        } catch (IOException e) {
            throw new ScaException(e);
        }
    }

    /**
     * Just like {@link com.l7tech.common.util.ProcUtils#exec(java.io.File, java.io.File, String[], byte[], boolean)} except
     * this always runs the command through sudo.
     *
     * @param cwd the current working directory in which to run the program, or null to inherit the current cwd.
     *            This is not necessarily the directory in which the program can be found -- just the cwd the
     *            subprocess shall be in before the program is invoked.
     * @param program  the program to run.  Required.
     * @param args the argument array.  May be empty but not null.
     * @param stdin  a byte array to pass into the program's stdin, or null to provide no input.
     * @param allowNonzeroExit  if true, this will return a result even if the program exits nonzero.
     *                          if false, IOException will be thrown if the program exits nonzero.
     * @return  the bytes that the program wrote to its stdout before exiting.  May be empty but never null.
     * @throws java.io.IOException if there is a problem running the subprocess, or the subprocess exits nonzero.
     * @see com.l7tech.common.util.ProcUtils#exec(java.io.File, java.io.File, String[], byte[], boolean)
     */
    private ProcResult exec(File cwd, File program, String[] args, byte[] stdin, boolean allowNonzeroExit) throws IOException {
        return ProcUtils.exec(cwd, sudo, args(program.getAbsolutePath(), args), stdin, allowNonzeroExit);
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
     * @throws ScaException if there is a problem storing this data.  The node's keystore may have been left
     *                      in an unusable state.
     */
    public void saveKeydata(byte[] keydata) throws ScaException {
        synchronized (globalMutex) {
            ProcResult result = null;
            try {
                doStopSca();
                beginKeydataChange();
                result = doSaveKeydata(keydata);
            } finally {
                if (result == null) {
                    // Failed to save new keydata -- try to restore old stuff
                    rollbackKeydataChange();
                } else {
                    commitKeydataChange();
                }
                try {
                    doStartSca();
                } catch (ScaException e) {
                    logger.log(Level.WARNING, "Unable to restart scakiod: " + ExceptionUtils.getMessage(e), e);
                }
            }
        }
    }

    /**
     * Wipe out all content of the keystore data for this node.  This should only be called by the configurator
     * after zeroizing the board to clear out any stale keystore data.
     * <p>
     * This method will shut down the sckiod while the data is being deleted and start it again afterwards.
     * Naturally no SSG that is using the HSM may be running while this is happening (or indeed until a new
     * valid keystore has been created).
     *
     * @throws ScaException if there is a problem wiping the data.  This node's keystore may have been left in
     *                      an unusable state.
     */
    public void wipeKeydata() throws ScaException {
        synchronized (globalMutex) {
            // Does not bother using begin/commit/rollback since we're just nuking the data anyway
            // TODO might need to check for symlink pointing at .old though (or some other unusual state)
            try {
                doStopSca();
                doWipeKeydata();
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
     * Prepare to replace the keydata directory with new data.
     * <p><b>NOTE:</b>scakiod must not be running when this is called.</p>
     */
    private void beginKeydataChange() {
        // TODO change this so it is actually safe
        // - if symlink is pointing at ".old", delete any ".current" then copy ".old" to ".current", and snap symlink to ".current"
        // - copy ".current" to ".new"
        // - make sure save is changed so it writes into ".new"

        // Invariants:
        //  - symlink always points to valid data
        //  - if begin completes normally, we always leave symlink pointing at ".current" with a copy of it in ".new" waiting
        //    to be overwritten/modified, and possibly some old data in ".old"
    }

    /**
     * Commit a successfully-written keydata directory.
     * <p><b>NOTE:</b>scakiod must not be running when this is called.</p>
     */
    private void commitKeydataChange() {
        // TODO change this so it is actually safe
        // - if symlink is pointing at ".old", delete any ".current" then copy ".old" to ".current", and snap symlink to ".current"
        // - delete any existing ".old"
        // - copy ".current" to ".old"
        // - snap symlink to point at ".old"
        // - delete ".current" and rename ".new" to ".current"
        // - snap symlink to point at ".current"

        // Invariants:
        //  - symlink always points to valid data
        //  - if commit completes normally, we always leave symlink pointing at ".current" with the previous data in ".old"
    }

    /**
     * Roll back an unsuccessfully-written keydata directory.
     * <p><b>NOTE:</b>scakiod must not be running when this is called.</p>
     */
    private void rollbackKeydataChange() {
        // TODO change so it is actually safe
        // - delete any ".new" directory
    }
}

