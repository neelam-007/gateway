package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.api.ServiceMOAccessor;

/**
 *
 */
public class ServiceMOAccessorImpl extends PolicyAccessorImpl<ServiceMO> implements ServiceMOAccessor {

    //- PUBLIC

    @Override
    public ServiceDetail getServiceDetail( final String identifier ) throws AccessorException {
        require( "identifier", identifier );

        return getFragment( ServiceDetail.class, identifier, XPATH_DETAIL );
    }

    @Override
    public void putServiceDetail( final String identifier,
                                  final ServiceDetail serviceDetail ) throws AccessorException {
        require( "identifier", identifier );
        require( "serviceDetail", serviceDetail );

        putFragment( serviceDetail, identifier, XPATH_DETAIL );
    }

    @Override
    public ResourceSet getWsdl( final String identifier ) throws AccessorException {
        require( "identifier", identifier );

        return getFragment( ResourceSet.class, identifier, XPATH_WSDL );
    }

    @Override
    public void putWsdl( final String identifier,
                         final ResourceSet resourceSet ) throws AccessorException {
        require( "identifier", identifier );
        require( "resourceSet", resourceSet );

        putFragment( resourceSet, identifier, XPATH_DETAIL );

    }

    //- PACKAGE

    ServiceMOAccessorImpl( final String url,
                           final String resourceUri,
                           final Class<ServiceMO> typeClass,
                           final ResourceTracker resourceTracker ) {
        super( url, resourceUri, typeClass, resourceTracker );
    }

    //- PRIVATE

    private static final String XPATH_DETAIL = "l7:ServiceDetail";
    private static final String XPATH_WSDL = "l7:Resources/l7:ResourceSet[@tag='wsdl']";

}
