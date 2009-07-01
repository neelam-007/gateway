package com.l7tech.server.security.keystore.luna;

import com.l7tech.util.ExceptionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStoreException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class LunaProber {
    private static final Logger logger = Logger.getLogger(LunaProber.class.getName());

    private final boolean lunaClientLibraryAvailable;

    public LunaProber() {
        this.lunaClientLibraryAvailable = probeForLunaClientLibrary();
    }

    private static boolean probeForLunaClientLibrary() {
        try {
            Class prober = getProberClass();
            Method probe = prober.getMethod("probeForLunaClientLibrary");
            return Boolean.TRUE.equals(probe.invoke(null));

        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Luna client libraries not present or not configured: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error checking for Luna client libraries: " + ExceptionUtils.getMessage(t), t);
            return false;
        }
    }

    private static Class getProberClass() throws ClassNotFoundException {
        return Class.forName("com.l7tech.security.prov.luna.LunaProberImpl");
    }

    public static boolean isLunaClientLibraryAvailable() {
        return Holder.INSTANCE.lunaClientLibraryAvailable;
    }

    public static void testHardwareTokenAvailability(int slotNum, char[] tokenPin) throws KeyStoreException {
        if (!isLunaClientLibraryAvailable())
            throw new KeyStoreException("SafeNet HSM client software and JSP either not installed or not configured");

        try {
            Class prober = getProberClass();
            Method tester = prober.getMethod("testHardwareTokenAvailability", int.class, char[].class);
            tester.invoke(null, slotNum, tokenPin);
            
        } catch (ClassNotFoundException e) {
            throw new KeyStoreException(ExceptionUtils.getMessage(e), e);
        } catch (NoSuchMethodException e) {
            throw new KeyStoreException(ExceptionUtils.getMessage(e), e);
        } catch (InvocationTargetException e) {
            throw new KeyStoreException(ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new KeyStoreException(ExceptionUtils.getMessage(e), e);
        }
    }

    private static class Holder {
        private static final LunaProber INSTANCE = new LunaProber();
    }
}
