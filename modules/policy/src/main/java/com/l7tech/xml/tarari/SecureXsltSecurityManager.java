package com.l7tech.xml.tarari;

import com.tarari.xml.xslt11.XsltSecurityManager;
import com.tarari.xml.xslt11.profiler.web.ServletXsltSecurityManager;

/**
 * XsltSecurityManager that doesn't allow anything.
 *
 * <p>Ensure you hold a reference to the instance to prevent GC.</p>
 *
 * @author Steve Jones
 */
public final class SecureXsltSecurityManager extends ServletXsltSecurityManager {

    /**
     * Ensure never replaced
     *
     * @param manager ignored
     * @throws SecurityException always
     */
    public final void checkReplace(XsltSecurityManager manager) throws SecurityException {
        if (manager != this)
            throw new SecurityException("SecureXsltSecurityManager cannot be replaced.");
    }
}
