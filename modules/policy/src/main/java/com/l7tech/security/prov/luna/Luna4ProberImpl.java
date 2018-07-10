package com.l7tech.security.prov.luna;

import com.chrysalisits.crypto.LunaTokenManager;
import com.l7tech.util.ExceptionUtils;

import java.security.KeyStoreException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds Luna-specific code for checking to see if Luna client libraries are available and configured.
 */
public class Luna4ProberImpl {
    private static final Logger logger = Logger.getLogger(Luna4ProberImpl.class.getName());

    public static boolean probeForLunaClientLibrary() {
        return isAtLeastOneTokenPresent();
    }

    private synchronized static boolean isAtLeastOneTokenPresent() {
        boolean sawTokenPresent = false;
        LunaTokenManager tm = LunaTokenManager.getInstance();
        int numSlots = tm.GetNumberOfSlots();
        for (int i = 1; i < numSlots + 1; i++) {
            if (tm.IsTokenPresent(i)) {
                logger.info("Luna Token #" + i + ": " + tm.GetTokenLabel(i) + ": present");
                sawTokenPresent = true;
            } else {
                logger.info("Luna Token #" + i + ": not present");
            }
        }
        if (!sawTokenPresent)
            logger.info("No Luna v4 tokens are present");
        return sawTokenPresent;
    }

    public synchronized static void testHardwareTokenAvailability(int slotNum, char[] pin) throws KeyStoreException {
        LunaTokenManager tm = LunaTokenManager.getInstance();
        if (tm.isLoggedIn())
            throw new KeyStoreException("SafeNet HSM is already in use");
        try {
            if (slotNum >= 1) {
                LunaTokenManager.getInstance().Login(slotNum, new String(pin));
            } else {
                LunaTokenManager.getInstance().Login(new String(pin));
            }
        } finally {
            try {
                if (tm.isLoggedIn())
                    tm.Logout();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unexpected exception logging out of Luna token: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }
}
