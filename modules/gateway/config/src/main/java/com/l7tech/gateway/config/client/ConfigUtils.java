package com.l7tech.gateway.config.client;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JceUtil;
import com.l7tech.util.SyspropUtil;

import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * These are shared utilities for the config classes
 */
final class ConfigUtils {
    private static final Logger logger = Logger.getLogger(ConfigUtils.class.getName());

    private static final boolean PERFORM_CRYPTO_CHECK = SyspropUtil.getBoolean("com.l7tech.gateway.config.checkStrongCryptography", true);

    static boolean isStrongCryptoEnabledInJvm() {
        try {
            return !PERFORM_CRYPTO_CHECK || JceUtil.isStrongCryptoEnabledInJvm();
        } catch (GeneralSecurityException e) {
            logger.log(
                    Level.WARNING,
                    "Error determining if strong cryptography is enabled: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            return true;
        }
    }
}
