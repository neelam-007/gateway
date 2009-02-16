/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.gateway.common.License;

import java.util.logging.Logger;

/**
 * Keeps track of license permissions.
 */
public class GatewayLicenseManager extends AbstractLicenseManager {

    private static final Logger logger = Logger.getLogger( GatewayLicenseManager.class.getName() );

    public GatewayLicenseManager(ClusterPropertyManager clusterPropertyManager)
    {
        super(logger, clusterPropertyManager);
    }

    @Override
    protected License.FeatureSetExpander getFeatureSetExpander()
    {
        return GatewayFeatureSets.getFeatureSetExpander();
    }
}
