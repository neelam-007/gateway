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

    private static final String PROBER_V4_IMPL_CLASSNAME = "com.l7tech.security.prov.luna.Luna4ProberImpl";
    private static final String PROBER_V5_IMPL_CLASSNAME = "com.l7tech.security.prov.luna.Luna5ProberImpl";

    private final boolean lunaV4ClientLibraryAvailable;
    private final boolean lunaV5ClientLibraryAvailable;

    public LunaProber() {
        this.lunaV4ClientLibraryAvailable = probeForLunaClientLibrary("V4", PROBER_V4_IMPL_CLASSNAME);
        this.lunaV5ClientLibraryAvailable = probeForLunaClientLibrary("V5", PROBER_V5_IMPL_CLASSNAME);
    }

    private static boolean probeForLunaClientLibrary(String ver, String classname) {
        try {
            Class prober = Class.forName(classname);
            Method probe = prober.getMethod("probeForLunaClientLibrary");
            return Boolean.TRUE.equals(probe.invoke(null));

        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Luna " + ver + " client libraries not present or not configured: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        } catch (UnsatisfiedLinkError e) {
            logger.log(Level.WARNING, "Luna " + ver + " client libraries not present or not configured: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        } catch (InvocationTargetException e) {
            if (ExceptionUtils.causedBy(e, ClassNotFoundException.class) || ExceptionUtils.causedBy(e, UnsatisfiedLinkError.class)) {
                logger.log(Level.WARNING, "Luna " + ver + " client libraries not present or not configured: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return false;
            }
            logger.log(Level.SEVERE, "Error checking for Luna " + ver + " client libraries: " + ExceptionUtils.getMessage(e), e);
            return false;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error checking for Luna " + ver + " client libraries: " + ExceptionUtils.getMessage(t), t);
            return false;
        }
    }

    public static boolean isLunaClientLibraryAvailable() {
        return Holder.INSTANCE.lunaV4ClientLibraryAvailable || Holder.INSTANCE.lunaV5ClientLibraryAvailable;
    }

    public static void testHardwareTokenAvailability(int slotNum, char[] tokenPin) throws KeyStoreException {
        if (!isLunaClientLibraryAvailable())
            throw new KeyStoreException("SafeNet HSM client software and JSP either not installed or not configured");

        try {
            String classname = Holder.INSTANCE.lunaV5ClientLibraryAvailable ? PROBER_V5_IMPL_CLASSNAME : PROBER_V4_IMPL_CLASSNAME;
            Class prober = Class.forName(classname);
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
