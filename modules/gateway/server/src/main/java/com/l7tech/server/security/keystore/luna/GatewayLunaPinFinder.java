package com.l7tech.server.security.keystore.luna;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.SyspropUtil;

import java.text.ParseException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class GatewayLunaPinFinder implements Callable<char[]> {
    private static final Logger logger = Logger.getLogger(GatewayLunaPinFinder.class.getName());

    private static MasterPasswordManager lunaPinEncryption;

    // LunaJceProviderEngine requires a pin finder to have a public nullary constructor
    public GatewayLunaPinFinder() {
    }

    @Override
    public char[] call() throws Exception {
        char[] pin = getLunaPin();
        // Clear it after it is queried so custom assertions/etc can't read it
        System.clearProperty("com.l7tech.encryptedLunaPin");
        return pin;
    }

    public static void setClusterPassphrase(char[] clusterPassphrase) {
        if (lunaPinEncryption != null)
            throw new IllegalStateException("Luna PIN encryption already set");
        lunaPinEncryption = new MasterPasswordManager(clusterPassphrase);
    }

    public static char[] getLunaPin() {
        String encrypted = SyspropUtil.getString("com.l7tech.encryptedLunaPin", null);
        if (encrypted == null)
            throw new RuntimeException("No encrypted Luna PIN available");
        if (lunaPinEncryption == null)
            throw new RuntimeException("Unable to decrypt Luna PIN: Luna PIN encryption not yet set");
        try {
            return lunaPinEncryption.looksLikeEncryptedPassword(encrypted)
                    ? lunaPinEncryption.decryptPassword(encrypted)
                    : encrypted.toCharArray();
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Unable to decrypt com.l7tech.encryptedLunaPin value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return null;
        }
    }

    public static String encryptLunaPin(char[] plaintext) {
        if (lunaPinEncryption == null)
            throw new RuntimeException("Unable to encrypt Luna PIN: Luna PIN encryption not yet set");
        return lunaPinEncryption.encryptPassword(plaintext);
    }
}
