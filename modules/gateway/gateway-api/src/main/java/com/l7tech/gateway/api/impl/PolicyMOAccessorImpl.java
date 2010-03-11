package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.PolicyDetail;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.PolicyMOAccessor;
import com.sun.ws.management.client.ResourceFactory;

/**
 *
 */
public class PolicyMOAccessorImpl extends PolicyAccessorImpl<PolicyMO> implements PolicyMOAccessor {

    //- PUBLIC

    @Override
    public PolicyDetail getPolicyDetail( final String identifier ) throws AccessorException {
        require( "identifier", identifier );

        return getFragment( PolicyDetail.class, identifier, XPATH_DETAIL );
    }

    @Override
    public void putPolicyDetail( final String identifier,
                                 final PolicyDetail policyDetail ) throws AccessorException {
        require( "identifier", identifier );
        require( "policyDetail", policyDetail );

        putFragment( policyDetail, identifier, XPATH_DETAIL );
    }

    //- PACKAGE

    PolicyMOAccessorImpl( final String url,
                          final String resourceUri,
                          final Class<PolicyMO> typeClass,
                          final ResourceFactory resourceFactory,
                          final ResourceTracker resourceTracker ) {
        super( url, resourceUri, typeClass, resourceFactory, resourceTracker );
    }

    //- PRIVATE

    private static final String XPATH_DETAIL = "l7:PolicyDetail";

}
