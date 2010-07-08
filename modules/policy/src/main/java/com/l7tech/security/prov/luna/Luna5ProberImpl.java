package com.l7tech.security.prov.luna;

import com.l7tech.util.ExceptionUtils;
import com.safenetinc.luna.LunaSlotManager;

import java.security.KeyStoreException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Luna5ProberImpl {
    private static final Logger logger = Logger.getLogger(Luna5ProberImpl.class.getName());

    public static boolean probeForLunaClientLibrary() {
        return isAtLeastOneTokenPresent();
    }

    private synchronized static boolean isAtLeastOneTokenPresent() {
        boolean sawTokenPresent = false;
        LunaSlotManager tm = LunaSlotManager.getInstance();
        int numSlots = tm.getNumberOfSlots();
        for (int i = 1; i < numSlots + 1; i++) {
            if (tm.isTokenPresent(i)) {
                logger.info("Luna Token #" + i + ": " + tm.getTokenLabel(i) + ": present");
                sawTokenPresent = true;
            } else {
                logger.info("Luna Token #" + i + ": not present");
            }
        }
        if (!sawTokenPresent)
            logger.info("No Luna v5 tokens are present");
        return sawTokenPresent;
    }

    public synchronized static void testHardwareTokenAvailability(int slotNum, char[] pin) throws KeyStoreException {
        LunaSlotManager tm = LunaSlotManager.getInstance();
        if (tm.isLoggedIn())
            throw new KeyStoreException("SafeNet HSM is already in use");
        try {
            if (slotNum >= 1) {
                tm.login(slotNum, new String(pin));
            } else {
                tm.login(new String(pin));
            }
        } finally {
            try {
                if (tm.isLoggedIn())
                    tm.logout();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unexpected exception logging out of Luna token: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }
}
