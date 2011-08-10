package com.l7tech.security.prov.ncipher;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.SyspropUtil;
import com.ncipher.provider.km.nCipherKM;

import java.security.Provider;
import java.security.Security;

/**
 *
 */
public class NcipherJceProviderEngine extends JceProvider {
    final Provider PROVIDER;
    final Provider MESSAGE_DIGEST_PROVIDER;

    public NcipherJceProviderEngine() {
        if ( null == SyspropUtil.getProperty( "protect" ) ) {
            // Prefer module-level key protection by default
            SyspropUtil.setProperty( "protect", "module" );
        }
        Provider existing =  Security.getProvider("nCipherKM");
        if (null == existing) {
            // Insert nCipher as highest-preference provider
            PROVIDER = new nCipherKM();
            Security.insertProviderAt(PROVIDER, 1);
        } else {
            // Leave existing provider order unchanged
            PROVIDER = existing;
        }
        PROVIDER.remove("KeyStore.JKS"); // Bug #10109 - avoid message to STDERR from nCipher prov when third-party software loads truststores using JKS
        MESSAGE_DIGEST_PROVIDER = Security.getProvider("SUN"); // Bug #10327 - use Sun JDK provider for MD5, SHA-1, and SHA-2, if available, to avoid clobbering a nethsm with Sha512Crypt password hashes
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.getName();
    }

    @Override
    protected String getRsaNoPaddingCipherName() {
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
        return super.getProviderFor(service);
    }
}
