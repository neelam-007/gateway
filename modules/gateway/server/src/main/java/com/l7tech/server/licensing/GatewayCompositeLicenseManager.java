package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.FeatureSetExpander;
import com.l7tech.server.GatewayFeatureSets;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
@Component
public class GatewayCompositeLicenseManager extends AbstractCompositeLicenseManager {

    private static final Logger logger = Logger.getLogger(GatewayCompositeLicenseManager.class.getName());

    public GatewayCompositeLicenseManager() {
        super(logger);
    }

    @Override
    protected FeatureSetExpander getFeatureSetExpander() {
        return GatewayFeatureSets.getFeatureSetExpander();
    }
}
