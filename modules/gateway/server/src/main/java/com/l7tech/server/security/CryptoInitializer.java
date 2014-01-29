package com.l7tech.server.security;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.system.Initialized;
import com.l7tech.server.event.system.Starting;
import com.l7tech.util.Config;
import com.l7tech.util.SyspropUtil;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.security.SecureRandom;
import java.util.logging.Logger;

/**
 * Bean that ensures any needed crypto system properties are populated before JceProvider is initialized and locked
 * in.
 */
public class CryptoInitializer implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(CryptoInitializer.class.getName());

    private final Config config;
    private final LicenseManager licenseManager;
    private boolean permafips;

    public CryptoInitializer(Config config, LicenseManager licenseManager) {
        this.config = config;
        this.licenseManager = licenseManager;
        init();
    }

    private void init() {
        // Check for FIPS flag
        config.getBooleanProperty( ServerConfigParams.PARAM_FIPS, false);
        if (licenseManager.isFeatureEnabled(GatewayFeatureSets.FLAG_PERMAFIPS)) {
            permafips = true;
            SyspropUtil.setProperty("com.l7tech.security.fips.alwaysEnabled", "true");
        }
        // We won't actually call JceProvider.init() or JceProvider.getInstance() yet,
        // since we need to give the override jce provider name cluster property a chance to load first
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof Starting) {
            if (permafips && !JceProvider.getInstance().isFips140ModeEnabled()) {
                String msg = "FATAL: FIPS 140 mode is required by the current license but could not be enabled using the current cryptography provider settings.";
                logger.severe(msg);
                throw new IllegalStateException(msg);
            }

            // Double check random number generator is not Dual EC DRBG
            if ( SyspropUtil.getBoolean( "com.l7tech.security.rng.blacklist.ecdrbg", true ) && new SecureRandom().getAlgorithm().toUpperCase().trim().contains( "ECDRBG" ) ) {
                final String msg = "FATAL: Default SecureRandom algorithm appears to be Dual EC DRBG, which is not permitted.  Set system property com.l7tech.security.rng.blacklist.ecdrbg=false to override";
                logger.severe( msg );
                throw new RuntimeException( msg );
            }

            WssProcessorAlgorithmFactory.clearAlgorithmPools();
        } else if (applicationEvent instanceof Initialized) {
            WssProcessorAlgorithmFactory.clearAlgorithmPools();
        }
    }
}
