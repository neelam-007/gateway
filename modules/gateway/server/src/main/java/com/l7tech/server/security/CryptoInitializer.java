package com.l7tech.server.security;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.Starting;
import com.l7tech.util.SyspropUtil;
import com.rsa.jsse.JsseProvider;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.security.Provider;
import java.security.Security;
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
            SyspropUtil.setProperty("com.l7tech.security.fips.alwaysEnabled", "true");
        }

        if (JceProvider.getInstance().isComponentCompatible(JceProvider.COMPONENT_RSA_SSLJ_5_1_1)) {
            changeDefaultTlsProvider();
        } else {
            logger.info("TLS 1.2 not enabled.  The TLS 1.2 provider is not compatible with the current cryptography provider.");
        }
    }

    private void changeDefaultTlsProvider() {
        // Change default provider for TLS from SunJSSE to RsaJsse.
        // This will be used for all outbound TLS from the Gateway whenever it is not in FIPS mode.
        // For incoming TLS, the server socket factory will have to specify a provider each time it creates
        // an SSLContext (to avoid using RSA SSL-J in situations where it won't behave as desired).
        Provider sunjsse = Security.getProvider("SunJSSE");
        JsseProvider jsseProvider = new JsseProvider();
        Security.insertProviderAt(jsseProvider, 1);
        if (sunjsse != null) {
            // Move SunJSSE to the end.  We can't unregister it completely because we still need to use it
            // and, because of how it is implemented, it is non-functional unless registered as a security provider.
            Security.removeProvider("SunJSSE");
            Security.addProvider(sunjsse);
        }
        logger.info("Registered " + jsseProvider.getName() + " " + jsseProvider.getVersion() + " as default JSSE provider");
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
