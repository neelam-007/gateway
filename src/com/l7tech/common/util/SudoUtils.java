package com.l7tech.common.util;

import java.io.File;
import java.io.IOException;

/**
 * Utilities involving the Unix program sudo.
 */
public class SudoUtils {
    private static final String DEFAULT_SUDO_PATH = "/usr/bin/sudo";
    public static final String PROPERTY_SUDO_PATH = "com.l7tech.sudoPath";
    private static File sudo = null;

    /**
     * Find the sudo program on this system.
     *
     * @return a File pointing at the sudo binary.  Never null.
     * @throws IOException if a suitable sudo binary can't be found.
     */
    public static synchronized File findSudo() throws IOException {
        if (sudo != null)
            return sudo;
        sudo = FileUtils.findConfiguredFile("sudo", PROPERTY_SUDO_PATH, DEFAULT_SUDO_PATH, true, false, false, false, true);
        return sudo;
    }
}
