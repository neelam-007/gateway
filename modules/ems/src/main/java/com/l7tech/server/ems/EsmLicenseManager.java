package com.l7tech.server.ems;

import com.l7tech.gateway.common.licensing.FeatureSetExpander;
import com.l7tech.server.AbstractLicenseManager;
import com.l7tech.server.cluster.ClusterPropertyManager;

import java.util.logging.Logger;

/**
 * License Manager for ESM
 */
public class EsmLicenseManager extends AbstractLicenseManager {

    private static final Logger logger = Logger.getLogger( EsmLicenseManager.class.getName() );

    public EsmLicenseManager( final ClusterPropertyManager clusterPropertyManager ) {
        super(logger, clusterPropertyManager);
    }

    @Override
    protected FeatureSetExpander getFeatureSetExpander() {
        return EsmFeatureSets.getFeatureSetExpander();
    }
}
