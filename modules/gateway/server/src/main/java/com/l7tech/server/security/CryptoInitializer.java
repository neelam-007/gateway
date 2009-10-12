package com.l7tech.server.security;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.Starting;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.logging.Logger;

/**
 * Bean that ensures any needed crypto system properties are populated before JceProvider is initialized and locked
 * in.
 */
public class CryptoInitializer implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(CryptoInitializer.class.getName());

    private final ServerConfig serverConfig;
    private final LicenseManager licenseManager;
    private boolean permafips;

    public CryptoInitializer(ServerConfig serverConfig, LicenseManager licenseManager) {
        this.serverConfig = serverConfig;
        this.licenseManager = licenseManager;
        init();
    }

    private void init() {
        // Check for FIPS flag
        serverConfig.getBooleanProperty(ServerConfig.PARAM_FIPS, false);
        if (licenseManager.isFeatureEnabled(GatewayFeatureSets.FLAG_PERMAFIPS)) {
            permafips = true;
            System.setProperty("com.l7tech.security.fips.alwaysEnabled", "true");
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof Starting) {
            if (permafips && !JceProvider.getInstance().isFips140ModeEnabled()) {
                String msg = "FATAL: FIPS 140 mode is required by the current license but could not be enabled using the current cryptography provider settings.";
                logger.severe(msg);
                throw new IllegalStateException(msg);
            }
        }
    }
}
