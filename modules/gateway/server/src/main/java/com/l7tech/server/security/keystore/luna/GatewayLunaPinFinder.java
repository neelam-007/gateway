package com.l7tech.server.security.keystore.luna;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MasterPasswordManager;

import java.text.ParseException;
import java.util.Arrays;
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
        return getLunaPin();
    }

    public static void setClusterPassphrase(final byte[] clusterPassphrase) {
        if (lunaPinEncryption != null)
            throw new IllegalStateException("Luna PIN encryption already set");
        lunaPinEncryption = new MasterPasswordManager(new MasterPasswordManager.MasterPasswordFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return Arrays.copyOf( clusterPassphrase, clusterPassphrase.length );
            }
        });
    }

    public static char[] getLunaPin() {
        String encrypted = ConfigFactory.getProperty( "com.l7tech.encryptedLunaPin" );
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
