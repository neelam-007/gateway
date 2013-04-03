package com.l7tech.security.prov;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Provider;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provider utility class.
 */
public final class ProviderUtil {
    private static final Logger logger = Logger.getLogger(ProviderUtil.class.getName());
    private ProviderUtil() {
        // do not construct
    }

    /**
     * Configure the given provider by removing blacklisted services.
     *
     * Logs a warning if an error occurs.
     *
     * @param serviceBlacklist a collection of services to remove where each service is identified as a Pair (key = service type, value = service algorithm).
     * @param provider         the provider to configure.
     */
    public static void configureProvider(final Collection<Pair<String, String>> serviceBlacklist, final Provider provider) {
        try {
            final Method method = Provider.class.getDeclaredMethod("removeService", Provider.Service.class);
            method.setAccessible(true);

            for (Pair<String, String> serviceDesc : serviceBlacklist) {
                final String type = serviceDesc.left;
                final String algorithm = serviceDesc.right;
                final Provider.Service service = provider.getService(type, algorithm);
                if (service != null) { // may be null in some modes
                    logger.fine("Removing service '" + type + "." + algorithm + "'.");
                    method.invoke(provider, service);
                }
            }
        } catch (final InvocationTargetException e) {
            logger.log(Level.WARNING, "Error configuring services '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        } catch (final NoSuchMethodException e) {
            logger.log(Level.WARNING, "Error configuring services '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        } catch (final IllegalAccessException e) {
            logger.log(Level.WARNING, "Error configuring services '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
        }
    }
}
