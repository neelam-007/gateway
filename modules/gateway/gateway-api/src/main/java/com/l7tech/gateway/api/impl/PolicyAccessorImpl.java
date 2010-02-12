package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyAccessor;
import com.l7tech.gateway.api.PolicyExportResult;
import com.l7tech.gateway.api.PolicyImportResult;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.PolicyValidationContext;
import com.l7tech.gateway.api.PolicyValidationResult;
import com.l7tech.gateway.api.ServiceMO;
import com.sun.ws.management.client.Resource;
import com.sun.ws.management.client.ResourceFactory;
import com.sun.ws.management.client.ResourceState;
import com.sun.ws.management.client.exceptions.FaultException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
class PolicyAccessorImpl<MO extends ManagedObject> extends AccessorImpl<MO> implements PolicyAccessor<MO> {

    @Override
    public String exportPolicy( final String identifier ) throws AccessorException {
        require( "identifier", identifier );

        return invoke(new AccessorMethod<String>(){
            @Override
            public String invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        ResourceFactory.find( getUrl(), getResourceUri(), getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final ResourceState resourceState =
                        resource.invoke( buildActionUri("ExportPolicy"), newDocument() );

                return ManagedObjectFactory.read( resourceState.getDocument(), PolicyExportResult.class ).getResource().getContent();
            }
        });
    }

    @Override
    public PolicyImportResult importPolicy( final String identifier,
                                            final Map<String, Object> properties,
                                            final String export,
                                            final List<PolicyReferenceInstruction> instructions ) throws AccessorException {
        require( "identifier", identifier );
        require( "export", export );

        return invoke(new AccessorMethod<PolicyImportResult>(){
            @Override
            public PolicyImportResult invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        ResourceFactory.find( getUrl(), getResourceUri(), getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final PolicyImportContext context = new PolicyImportContext();
                context.setProperties( properties );
                context.setResource( ManagedObjectFactory.createResource() );
                context.setPolicyReferenceInstructions( instructions );

                context.getResource().setType( "policyexport" );
                context.getResource().setContent( export );

                final ResourceState resourceState =
                        resource.invoke( buildActionUri("ImportPolicy"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PolicyImportResult.class );
            }
        });
    }

    /**
     * 
     */
    @Override
    public PolicyValidationResult validatePolicy( final MO managedObject ) throws AccessorException {
        require( "managedObject", managedObject );

        final PolicyValidationContext context = ManagedObjectFactory.createPolicyValidationContext();
        if ( managedObject instanceof PolicyMO ) {
            PolicyMO policy = (PolicyMO) managedObject;
            require( "policyDetail", policy.getPolicyDetail() );
            require( "policyDetail.policyType", policy.getPolicyDetail().getPolicyType() );
            require( "resourceSets", policy.getResourceSets() );
            context.setPolicyType( policy.getPolicyDetail().getPolicyType() );
            context.setProperties( policy.getPolicyDetail().getProperties() );
            context.setResourceSets( policy.getResourceSets() );
        } else if ( managedObject instanceof ServiceMO ) {
            ServiceMO service = (ServiceMO) managedObject;
            require( "serviceDetail", service.getServiceDetail() );
            require( "resourceSets", service.getResourceSets() );
            context.setProperties( service.getServiceDetail().getProperties() );
            context.setResourceSets( service.getResourceSets() );
        } else {
            throw new AccessorException("Policy validation does not support '"+(managedObject==null ? "<null>" : managedObject.getClass().getName())+"'");
        }

        return invoke(new AccessorMethod<PolicyValidationResult>(){
            @Override
            public PolicyValidationResult invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        ResourceFactory.find( getUrl(), getResourceUri(), getTimeout(), (Map<String,String>)null)[0];

                final ResourceState resourceState =
                        resource.invoke( buildActionUri("ValidatePolicy"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PolicyValidationResult.class );
            }
        });
    }

    //- PACKAGE

    PolicyAccessorImpl( final String url,
                        final String resourceUri,
                        final Class<MO> typeClass,
                        final ResourceTracker resourceTracker ) {
        super( url, resourceUri, typeClass, resourceTracker );
    }
}
