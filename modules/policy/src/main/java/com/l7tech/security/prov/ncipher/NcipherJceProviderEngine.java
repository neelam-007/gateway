package com.l7tech.security.prov.ncipher;

import com.l7tech.security.prov.GcmCipher;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.ncipher.km.nfkm.Module;
import com.ncipher.km.nfkm.SecurityWorld;
import com.ncipher.nfast.NFException;
import com.ncipher.provider.km.nCipherKM;
import org.jetbrains.annotations.NotNull;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class NcipherJceProviderEngine extends JceProvider {
    private static final int UNDEFINED_POSITION = -1;
    final Provider PROVIDER;
    final Provider MESSAGE_DIGEST_PROVIDER;
    final Provider SOFTWARE_DH_PROVIDER;

    final long moduleId;

    private static final Logger logger = Logger.getLogger(NcipherJceProviderEngine.class.getName());

    // A GCM IV full of zero bytes, for sanity checking IVs
    private static final byte[] ZERO_IV = new byte[12];

    public NcipherJceProviderEngine() {
        if ( null == SyspropUtil.getProperty( "protect" ) ) {
            // Prefer module-level key protection by default
            SyspropUtil.setProperty( "protect", "module" );
        }

        final int specifiedPos = ConfigFactory.getIntProperty("com.l7tech.ncipher.position", UNDEFINED_POSITION);
        if (specifiedPos == UNDEFINED_POSITION) {
            // if no position explicitly specified, fall back to preference.
            boolean mostPref = "highest".equalsIgnoreCase(ConfigFactory.getProperty("com.l7tech.ncipher.preference", "default"));

            Provider existing =  Security.getProvider("nCipherKM");
            if (null == existing) {
                // Insert nCipher as highest-preference provider
                PROVIDER = new nCipherKM();
                if (mostPref) {
                    Security.insertProviderAt(PROVIDER, 1);
                } else {
                    Security.addProvider(PROVIDER);
                }
            } else {
                if (mostPref) {
                    PROVIDER = new nCipherKM();
                    Security.removeProvider(existing.getName());
                    Security.insertProviderAt(PROVIDER, 1);
                } else {
                    // Leave existing provider order unchanged
                    PROVIDER = existing;
                }
            }
        } else {
            logger.log(Level.FINE, "nCipherKM JCE Provider position requested: {0}", specifiedPos);

            Provider existing = Security.getProvider("nCipherKM");

            if (null == existing) {
                // If no position already defined, insert nCipher provider at specified position
                PROVIDER = new nCipherKM();

                // N.B. if adding as lowest priority, the actual position is not known until after statically defined providers have been loaded and this one appended
                int actualPos = Security.insertProviderAt(PROVIDER, specifiedPos);
                logger.log(Level.INFO, "nCipherKM JCE Provider inserted at position: {0}", actualPos);
            } else {
                // Remove existing nCipher provider and insert new instance at specified position
                PROVIDER = new nCipherKM();

                Security.removeProvider(existing.getName());
                logger.log(Level.INFO, "Existing nCipherKM JCE Provider removed");

                int actualPos = Security.insertProviderAt(PROVIDER, specifiedPos);
                logger.log(Level.INFO, "nCipherKM JCE Provider inserted at position: {0}", actualPos);
            }
        }

        PROVIDER.remove("KeyStore.JKS"); // Bug #10109 - avoid message to STDERR from nCipher prov when third-party software loads truststores using JKS
        MESSAGE_DIGEST_PROVIDER = Security.getProvider("SUN"); // Bug #10327 - use Sun JDK provider for MD5, SHA-1, and SHA-2, if available, to avoid clobbering a nethsm with Sha512Crypt password hashes
        SOFTWARE_DH_PROVIDER = Security.getProvider("SunJCE"); // Bug #11810 - use Sun JDK provider for Diffie-Hellman KeyPairGenerator for JSCAPE software SFTP
        try {
            moduleId = findFirstUsableModuleId();
        } catch (NFException e) {
            throw new RuntimeException("Unable to find usable nCipher module to connect to: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public GcmCipher getAesGcmCipherWrapper() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return new NcipherGcmCipher(moduleId);
    }

    @Override
    public AlgorithmParameterSpec generateAesGcmParameterSpec(int authTagLenBytes, @NotNull byte[] iv) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        // We'll just use an IvParameterSpec to hold the IV.  We'll always use a 16-byte auth tag and no additional authenticated data.

        if (authTagLenBytes != 16)
            throw new InvalidAlgorithmParameterException("GCM auth tag length must be 16 bytes when using this crypto provider");
        if (iv.length != 12)
            throw new InvalidAlgorithmParameterException("GCM IV must be exactly 12 bytes long");
        if (Arrays.equals(ZERO_IV, iv))
            throw new InvalidAlgorithmParameterException("GCM IV is entirely zero octets");

        return new IvParameterSpec(iv);
    }


    @Override
    public String getDisplayName() {
        return PROVIDER.getName();
    }

    @Override
    public String getRsaNoPaddingCipherName() {
        return "RSA/ECB/NoPadding";
    }

    @Override
    protected String getRsaOaepPaddingCipherName() {
        return "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    }

    @Override
    protected String getRsaPkcs1PaddingCipherName() {
        return "RSA/ECB/PKCS1Padding";
    }

    @Override
    public Provider getProviderFor(String service) {
        if (service.startsWith("MessageDigest."))
            return MESSAGE_DIGEST_PROVIDER;
        if (SERVICE_DIFFIE_HELLMAN_SOFTWARE.equals(service))
            return SOFTWARE_DH_PROVIDER;
        if (SERVICE_SIGNATURE_RSA_PRIVATE_KEY.equals(service) ||
                service.startsWith(SERVICE_KEY_PAIR_GENERATOR_RSA))
            return PROVIDER;
        return super.getProviderFor(service);
    }

    private static long findFirstUsableModuleId() throws NFException {
        SecurityWorld sw = nCipherKM.getSW();
        Module[] modules = sw.getModules();
        if (modules == null)
            throw new NFException("No usable nCipher module is connected");
        for (Module module : modules) {
            if (module.isUsable())
                return module.getID().value;
        }
        throw new NFException("No usable nCipher module is connected");
    }
}
