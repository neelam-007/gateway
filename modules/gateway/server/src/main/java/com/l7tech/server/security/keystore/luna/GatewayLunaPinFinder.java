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

    // TODO replace obfuscation by hardcoded passphrase with an actual PBE key derived from the cluster passphrase
    // This is currently complicated by how early in Spring context creation the LunaJceProviderEngine must initialize,
    // and by the inability to access the Spring nodeProperties from the JceProvider code path.
    // If we do fix this we will need to retain the ability to read legacy values obfuscated with the old passphrase.
    private static MasterPasswordManager getObfuscator() {
        return new MasterPasswordManager("apurghqpurtaerhasd;kflharituh35[93hgjna=[a84lna[84tyqasdasdnf,mnqty8yafgha".toCharArray());
    }

    public static char[] getLunaPin() {
        String encrypted = SyspropUtil.getString("com.l7tech.encryptedLunaPin", null);
        if (encrypted == null)
            throw new RuntimeException("No encrypted Luna PIN available");
        MasterPasswordManager obfuscator = getObfuscator();
        try {
            return obfuscator.looksLikeEncryptedPassword(encrypted)
                    ? obfuscator.decryptPassword(encrypted)
                    : encrypted.toCharArray();
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Unable to decrypt com.l7tech.encryptedLunaPin value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return null;
        }
    }

    public static String encryptLunaPin(char[] plaintext) {
        return getObfuscator().encryptPassword(plaintext);
    }
}
