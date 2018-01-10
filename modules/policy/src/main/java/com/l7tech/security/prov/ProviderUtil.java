package com.l7tech.security.prov;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.safelogic.cryptocomply.jce.provider.SLProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Provider;
import java.security.Security;
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

    /**
     * If a method "removeService" throws an exception, then use this method to remove some services from a given provider.
     *
     * @param serviceBlacklist: a list of services to be removed.
     * @param provider: a crypto provider will remove a list of services.
     */
    public static void removeService(final Collection<Pair<String, String>> serviceBlacklist, final Provider provider) {
        for (Pair<String, String> serviceDesc : serviceBlacklist) {
            provider.remove(serviceDesc.left + "." + serviceDesc.right);
        }
    }

    /**
     * Pad leading zeros at the beginning of the decryption output, if the output byte length is less than the key size
     * in bytes, when the encryption/decryption is using "RSA/ECB/NoPadding".
     *
     * @param resultToBePadded: the byte array needs to be padded with leading zeros.
     * @param keySizeInBytes: the RSA key size in bytes.
     * @return a new array with padding zeros.
     */
    public static byte[] paddingDecryptionOutputUsingRsaEcbNoPadding(@NotNull final byte[] resultToBePadded, final int keySizeInBytes) {
        final int resultSizeInBytes = resultToBePadded.length;
        if (resultSizeInBytes < keySizeInBytes) {
            final byte[] resultWithPadding = new byte[keySizeInBytes];
            System.arraycopy(resultToBePadded, 0, resultWithPadding, (keySizeInBytes - resultSizeInBytes), resultSizeInBytes);
            return resultWithPadding;
        } else {
            return resultToBePadded;
        }
    }

    /**
     * Configure CCJ Crypto Provider such as making it be the most-preferred provider and replacing SecureRandomSpi
     * engine with HMacSP800DRBGResync.
     *
     * @param ccjProvider: the CCJ Crypto Provider to be configured
     */
    public static void configureCcjProvider(@NotNull final Provider ccjProvider) {
        if (! (ccjProvider instanceof SLProvider)) {
            throw new IllegalArgumentException("Found unexpected provider to configure.");
        }

        Security.removeProvider(SLProvider.PROVIDER_NAME);
        Security.insertProviderAt(ccjProvider, 1);

        // Fixing DE319350: "9.2 CR3 and above CA API Gateway Policy Manager Crashing Java OutOfBoundsException 8"
        // https://rally1.rallydev.com/#/56296576307d/detail/defect/160779212412
        //
        // CryptoComply® Java version 2 was not designed for simultaneous use in multiple threads. Prior to Java SE
        // 8u112, the Java Cryptography Extension included a thread safety wrapper, but this was removed from the
        // OpenJDK codebase in changeset c4ab046992ff.  Fortunately, CCJ uses the following line to manually introduce
        // a replacement thread safety wrapper, HMacSP800DRBGResync.
        ccjProvider.put("SecureRandom.HMacSP800", "com.safelogic.cryptocomply.crypto.prng.drbg.HMacSP800DRBGResync");
    }
}
