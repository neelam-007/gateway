package com.l7tech.common.security.socket;

import com.l7tech.common.util.Functions;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.common.util.ExceptionUtils;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A factory that knows how to find an appropriate LocalTcpPeerIdentifier implementation for the current environment.
 */
public class LocalTcpPeerIdentifierFactory {
    private static final Logger logger = Logger.getLogger(LocalTcpPeerIdentifierFactory.class.getName());

    public static final String PROP_IMPL_CLASSNAME = "com.l7tech.security.localTcpPeerIdentifier";
    static final String IMPL_CLASSNAME_WIN32 = "com.l7tech.common.security.socket.Win32LocalTcpPeerIdentifier";
    static final String IMPL_CLASSNAME = SyspropUtil.getString(PROP_IMPL_CLASSNAME, IMPL_CLASSNAME_WIN32);

    private static final String NOSERV = "No LocalTcpPeerIdentifier service available";
    private static final String NOSERVCOL = NOSERV + ": ";


    /**
     * Check if a LocalTcpPeerIdentifier implementation is available for the current environment.
     *
     * @return true if {@link #createIdentifier} would succeed. 
     */
    public static boolean isAvailable() {
        return Prober.factory != null;
    }


    /**
     * Create a new LocalTcpPeerIdentifier instance for the current environment.
     *
     * @return a new LocalTcpPeerIdentifier instance.  Never null.
     * @throws IllegalStateException if no service is available.  To avoid this, check {@link #isAvailable()}.
     */
    public static LocalTcpPeerIdentifier createIdentifier() throws IllegalStateException {
        final Functions.Nullary<LocalTcpPeerIdentifier> factory = Prober.factory;
        if (factory == null)
            throw new IllegalStateException(NOSERV);
        return factory.call();
    }


    private static class Prober {
        private static final Functions.Nullary<LocalTcpPeerIdentifier> factory = makeFactory();

        private static Functions.Nullary<LocalTcpPeerIdentifier> makeFactory() {
            if (IMPL_CLASSNAME == null || IMPL_CLASSNAME.trim().length() < 1) {
                // cleanly disabled
                logger.log(Level.FINE, NOSERVCOL + "feature disabled");
                return null;
            }

            try {
                Class implClass = Class.forName(IMPL_CLASSNAME);
                Object objInst = implClass.newInstance();
                if (!(objInst instanceof LocalTcpPeerIdentifier)) {
                    logger.log(Level.WARNING, NOSERVCOL + "system property " + PROP_IMPL_CLASSNAME + ": class is not assignable to LocalTcpPeerIdentifier");
                    return null;
                }

                // If prototype instance creates successfully, we'll build a factory
                final LocalTcpPeerIdentifier identifier = (LocalTcpPeerIdentifier) objInst;
                return new Functions.Nullary<LocalTcpPeerIdentifier>() {
                    public LocalTcpPeerIdentifier call() {
                        try {
                            return identifier.getClass().newInstance();
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

            } catch (LinkageError e) {
                // For now, since we don't check the OS before trying to load the default impl, this will be the
                // normal code path on systems other than Windows.
                logger.log(Level.FINE, NOSERVCOL + "native support unavailable");
                return null;
            } catch (Throwable t) {
                logger.log(Level.WARNING, NOSERVCOL + ExceptionUtils.getMessage(t), t);
                return null;
            }
        }
    }
}
