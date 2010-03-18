package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyAccessor;
import com.l7tech.gateway.api.PolicyExportResult;
import com.l7tech.gateway.api.PolicyImportResult;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.PolicyReferenceInstruction;
import com.l7tech.gateway.api.PolicyValidationContext;
import com.l7tech.gateway.api.PolicyValidationResult;
import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.util.Functions;
import com.sun.ws.management.client.Resource;
import com.sun.ws.management.client.ResourceFactory;
import com.sun.ws.management.client.ResourceState;
import com.sun.ws.management.client.exceptions.FaultException;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
class PolicyAccessorImpl<AO extends AccessibleObject> extends AccessorImpl<AO> implements PolicyAccessor<AO> {

    //- PUBLIC

    @Override
    public com.l7tech.gateway.api.Resource getPolicy( final String identifier ) throws AccessorException {
        require( "identifier", identifier );

        return getFragment( com.l7tech.gateway.api.Resource.class, identifier, XPATH_RESOURCE );
    }

    @Override
    public void putPolicy( final String identifier,
                           final com.l7tech.gateway.api.Resource policyResource ) throws AccessorException {
        require( "identifier", identifier );
        require( "resource", policyResource );

        putFragment( policyResource, identifier, XPATH_RESOURCE );
    }

    @Override
    public String exportPolicy( final String identifier ) throws AccessorException {
        require( "identifier", identifier );

        return invoke(new AccessorMethod<String>(){
            @Override
            public String invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("ExportPolicy"), newDocument() );

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
                        getResourceFactory().find( getUrl(), getResourceUri(), getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final PolicyImportContext context = new PolicyImportContext();
                context.setProperties( properties );
                context.setResource( ManagedObjectFactory.createResource() );
                context.setPolicyReferenceInstructions( instructions );

                context.getResource().setType( "policyexport" );
                context.getResource().setContent( export );

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("ImportPolicy"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PolicyImportResult.class );
            }
        });
    }

    @Override
    public PolicyValidationResult validatePolicy( final String identifier ) throws AccessorException {
        require( "identifier", identifier );

        return doValidatePolicy( 
                Collections.singletonMap(ID_SELECTOR, identifier),
                new Functions.NullaryThrows<Document,IOException>(){
            @Override
            public Document call() {
                return newDocument();
            }
        } );
    }

    /**
     * 
     */
    @Override
    public PolicyValidationResult validatePolicy( final AO managedObject,
                                                  final List<ResourceSet> resourceSets ) throws AccessorException {
        require( "managedObject", managedObject );

        final PolicyValidationContext context = ManagedObjectFactory.createPolicyValidationContext();
        if ( managedObject instanceof PolicyMO ) {
            PolicyMO policy = (PolicyMO) managedObject;
            require( "policyDetail", policy.getPolicyDetail() );
            require( "policyDetail.policyType", policy.getPolicyDetail().getPolicyType() );
            require( "resourceSets", policy.getResourceSets() );
            context.setPolicyType( policy.getPolicyDetail().getPolicyType() );
            context.setProperties( Collections.<String,Object>singletonMap( "soap", isSoap(policy.getPolicyDetail().getProperties()) ));
            context.setResourceSets( combineResourceSets( policy.getResourceSets(), resourceSets ) );
        } else if ( managedObject instanceof ServiceMO ) {
            ServiceMO service = (ServiceMO) managedObject;
            require( "serviceDetail", service.getServiceDetail() );
            require( "resourceSets", service.getResourceSets() );
            context.setProperties( Collections.<String,Object>singletonMap( "soap", isSoap(service.getServiceDetail().getProperties()) ));
            context.setResourceSets( combineResourceSets( service.getResourceSets(), resourceSets ) );
        } else {
            throw new AccessorException("Policy validation does not support '"+(managedObject==null ? "<null>" : managedObject.getClass().getName())+"'");
        }

        return doValidatePolicy( null, new Functions.NullaryThrows<Document,IOException>(){
            @Override
            public Document call() throws IOException {
                return ManagedObjectFactory.write(context);
            }
        } );
    }

    //- PACKAGE

    PolicyAccessorImpl( final String url,
                        final String resourceUri,
                        final Class<AO> typeClass,
                        final ResourceFactory resourceFactory,
                        final ResourceTracker resourceTracker ) {
        super( url, resourceUri, typeClass, resourceFactory, resourceTracker );
    }

    <FO> FO getFragment( final Class<FO> fragmentClass,
                         final String identifier,
                         final String xpathExpression ) throws AccessorException {
        return invoke(new AccessorMethod<FO>(){
            @Override
            public FO invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final ResourceState resourceState =
                        resource.get( xpathExpression, Collections.singletonMap(XPATH_NS_PREFIX, getNamespace()) ,Resource.XPATH_DIALECT );

                return MarshallingUtils.unmarshalFragment( fragmentClass, resourceState.getDocument() );
            }
        });
    }

    <FO> void putFragment( final FO fragment,
                           final String identifier,
                           final String xpathExpression ) throws AccessorException {
        invoke(new AccessorMethod<Void>(){
            @Override
            public Void invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final DOMResult result = new DOMResult();
                MarshallingUtils.marshal( fragment, result, true );
                resource.put( result.getNode(), xpathExpression, Collections.singletonMap(XPATH_NS_PREFIX, getNamespace()) ,Resource.XPATH_DIALECT );

                return null;
            }
        });
    }

    //- PRIVATE

    private static final String XPATH_RESOURCE = "l7:Resources/l7:ResourceSet[@tag='policy']/l7:Resource";
    private static final String XPATH_NS_PREFIX = "l7";

    private boolean isSoap( final Map<String,Object> properties ) {
        boolean soap = false;

        if ( properties != null && properties.get( "soap" ) instanceof Boolean ) {
            soap = (Boolean) properties.get( "soap" );
        }

        return soap;
    }

    /**
     * Add any extra resources (e.g. include policy fragments) to the main resource sets.
     */
    private List<ResourceSet> combineResourceSets( final List<ResourceSet> resourceSets,
                                                   final List<ResourceSet> additionalResourceSets ) {
        final List<ResourceSet> combined;

        if ( additionalResourceSets == null ) {
            combined = resourceSets;
        } else if ( resourceSets == null ) {
            combined = additionalResourceSets;
        } else {
            // Combine the resource sets by adding resources from any additional resource sets
            // to the matching (by tag) resource set.
            // We are forgiving of any invalid data here, so the merging tolerates any missing,
            // invalid or repeated tags (though we don't attempt to "correct" invalid resource
            // sets)
            combined = new ArrayList<ResourceSet>();

            final Map<String,ResourceSet> resourceSetMap = getResourceSetMap( resourceSets );
            final Map<String,ResourceSet> additionalResourceSetMap = getResourceSetMap( additionalResourceSets );
            final Set<ResourceSet> handled = new HashSet<ResourceSet>();

            for ( final Map.Entry<String,ResourceSet> resourceSetEntry : resourceSetMap.entrySet() ) {
                handled.add( resourceSetEntry.getValue() );                                       
                final ResourceSet toAdd = additionalResourceSetMap.get( resourceSetEntry.getKey() );

                if ( toAdd != null ) {
                    handled.add( toAdd );

                    final List<com.l7tech.gateway.api.Resource> resources = new ArrayList<com.l7tech.gateway.api.Resource>();
                    final ResourceSet merged = ManagedObjectFactory.createResourceSet();
                    merged.setRootUrl( resourceSetEntry.getValue().getRootUrl() );
                    merged.setTag( resourceSetEntry.getValue().getTag() );
                    if ( resourceSetEntry.getValue().getResources() != null )
                        resources.addAll( resourceSetEntry.getValue().getResources() );
                    if ( toAdd.getResources() != null )
                        resources.addAll( toAdd.getResources() );
                    merged.setResources( resources );
                } else {
                    combined.add( resourceSetEntry.getValue() );
                }
            }

            for ( final ResourceSet resourceSet : resourceSets ) {
                if ( !handled.contains(resourceSet)) combined.add( resourceSet );
            }
            for ( final ResourceSet resourceSet : additionalResourceSets ) {
                if ( !handled.contains(resourceSet)) combined.add( resourceSet );
            }
        }

        return combined;
    }

    private Map<String,ResourceSet> getResourceSetMap( final List<ResourceSet> resourceSets ) {
        Map<String,ResourceSet> resourceSetMap = new HashMap<String,ResourceSet>();

        if ( resourceSets != null ) {
            for ( final ResourceSet resourceSet : resourceSets ) {
                final String tag = resourceSet.getTag();
                if ( tag != null && !resourceSetMap.containsKey( tag ) ) {
                    resourceSetMap.put( tag, resourceSet );
                }
            }
        }

        return resourceSetMap;
    }

    private PolicyValidationResult doValidatePolicy( final Map<String,String> selectorMap,
                                                     final Functions.NullaryThrows<Document,IOException> requestGetter ) throws AccessorException {
        return invoke(new AccessorMethod<PolicyValidationResult>(){
            @Override
            public PolicyValidationResult invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), getTimeout(), selectorMap)[0];

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("ValidatePolicy"), requestGetter.call() );

                return ManagedObjectFactory.read( resourceState.getDocument(), PolicyValidationResult.class );
            }
        });
    }
}
