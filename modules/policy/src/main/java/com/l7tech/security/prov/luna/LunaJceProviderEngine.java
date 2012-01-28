/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.security.prov.luna;

import com.l7tech.security.prov.DelegatingJceProvider;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ExceptionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A set of JCE settings, translations, and installation behavior for the SafeNet HSM (formerly SafeNet Luna) network attached HSM.
 */
public class LunaJceProviderEngine extends DelegatingJceProvider {
    private static final Logger logger = Logger.getLogger(LunaJceProviderEngine.class.getName());

    private static final String PROBER_V4_IMPL_CLASSNAME = "com.l7tech.security.prov.luna.Luna4ProberImpl";
    private static final String PROBER_V5_IMPL_CLASSNAME = "com.l7tech.security.prov.luna.Luna5ProberImpl";

    private static final String PROVIDER_V4_IMPL_CLASSNAME = "com.l7tech.security.prov.luna.Luna4JceProviderEngine";
    private static final String PROVIDER_V5_IMPL_CLASSNAME = "com.l7tech.security.prov.luna.Luna5JceProviderEngine";

    public LunaJceProviderEngine() {
        super(findDelegate());
    }

    private static JceProvider findDelegate() {
        if (probeForLunaClientLibrary("V5", PROBER_V5_IMPL_CLASSNAME))
            return makeProvider(PROVIDER_V5_IMPL_CLASSNAME);
        if (probeForLunaClientLibrary("V4", PROBER_V4_IMPL_CLASSNAME))
            return makeProvider(PROVIDER_V4_IMPL_CLASSNAME);
        throw new IllegalStateException("Luna client libraries not present or not configured correctly (tried looking for both Luna v4 and Luna v5 libs)");
    }

    private static JceProvider makeProvider(String classname) {
        try {
            return (JceProvider) Class.forName(classname).newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to instantiate provider class: " + classname + ": " + ExceptionUtils.getMessage(e), e); // can't happen
        } catch (InstantiationException e) {
            throw new IllegalStateException("Unable to instantiate provider class: " + classname + ": " + ExceptionUtils.getMessage(e), e); // can't happen
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate provider class: " + classname + ": " + ExceptionUtils.getMessage(e), e); // can't happen
        }
    }

    private static boolean probeForLunaClientLibrary(String ver, String classname) {
        try {
            Class prober = Class.forName(classname);
            Method probe = prober.getMethod("probeForLunaClientLibrary");
            return Boolean.TRUE.equals(probe.invoke(null));

        } catch (ClassNotFoundException e) {
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
}
