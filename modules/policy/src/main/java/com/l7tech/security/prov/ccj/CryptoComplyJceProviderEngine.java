package com.l7tech.security.prov.ccj;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.ProviderUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Pair;
import com.safelogic.cryptocomply.jce.provider.SLProvider;

import java.security.Provider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * CryptoComply JCE provider engine used by DefaultJceProviderEngine in FIPS mode.
 */
public class CryptoComplyJceProviderEngine extends JceProvider {

    private static final Provider PROVIDER = new SLProvider();

    private static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.ccj.disableServices";
    private static final boolean DISABLE_BLACKLISTED_SERVICES = ConfigFactory.getBooleanProperty(PROP_DISABLE_BLACKLISTED_SERVICES, true);
    public static final Collection<Pair<String,String>> SERVICE_BLACKLIST = Collections.unmodifiableCollection(Arrays.asList(
        new Pair<>( "CertificateFactory", "X.509" ),
        new Pair<>( "KeyStore", "PKCS12" ),
        new Pair<>( "CertPathBuilder", "PKIX" ),
        new Pair<>( "CertPathValidator", "PKIX" ),
        new Pair<>( "CertStore", "Collection" )
    ));

    public CryptoComplyJceProviderEngine() {
        ProviderUtil.configureCcjProvider(PROVIDER);
        if (DISABLE_BLACKLISTED_SERVICES) {
            ProviderUtil.removeService(SERVICE_BLACKLIST, PROVIDER);
        }
    }

    @Override
    public boolean isFips140ModeEnabled() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.getName();
    }

    public static Provider getProvider() {
        return PROVIDER;
    }
}