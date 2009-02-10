/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.gateway.common.License;

/**
 * Keeps track of license permissions.
 */
public class GatewayLicenseManager extends AbstractLicenseManager {

    public GatewayLicenseManager(ClusterPropertyManager clusterPropertyManager)
    {
        super(clusterPropertyManager);
    }

    protected License.FeatureSetExpander getFeatureSetExpander()
    {
        return GatewayFeatureSets.getFeatureSetExpander();
    }
}
