package com.l7tech.server.globalresources;

import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.OidEntityManagerStub;

/**
 * 
 */
public class HttpConfigurationManagerStub extends OidEntityManagerStub<HttpConfiguration,EntityHeader> implements HttpConfigurationManager {


    public HttpConfigurationManagerStub() {
    }

    public HttpConfigurationManagerStub( final HttpConfiguration... entitiesIn ) {
        super( entitiesIn );
    }

    @Override
    public Class<HttpConfiguration> getImpClass() {
        return HttpConfiguration.class;
    }
}
