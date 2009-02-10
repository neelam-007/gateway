package com.l7tech.server.ems;

import com.l7tech.server.AbstractLicenseManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.gateway.common.License;

/**
 * License Manager for ESM
 */
public class EsmLicenseManager extends AbstractLicenseManager {

    public EsmLicenseManager( final ClusterPropertyManager clusterPropertyManager ) {
        super(clusterPropertyManager);
    }

    protected License.FeatureSetExpander getFeatureSetExpander() {
        return EsmFeatureSets.getFeatureSetExpander();
    }
}
