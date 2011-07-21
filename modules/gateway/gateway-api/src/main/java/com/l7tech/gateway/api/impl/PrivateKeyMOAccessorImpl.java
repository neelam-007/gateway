package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PrivateKeyCreationContext;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.api.PrivateKeyMOAccessor;
import com.sun.ws.management.client.Resource;
import com.sun.ws.management.client.ResourceFactory;
import com.sun.ws.management.client.ResourceState;
import com.sun.ws.management.client.exceptions.FaultException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 *
 */
public class PrivateKeyMOAccessorImpl  extends AccessorImpl<PrivateKeyMO> implements PrivateKeyMOAccessor {

    //- PUBLIC

    @Override
    public PrivateKeyMO setSpecialPurposes( final String identifier,
                                            final Set<String> specialPurposes ) throws AccessorException {
        require( "identifier", identifier );
        require( "specialPurposes", specialPurposes );

        return invoke(new AccessorMethod<PrivateKeyMO>(){
            @Override
            public PrivateKeyMO invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), (long) getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final PrivateKeySpecialPurposeContext context = new PrivateKeySpecialPurposeContext();
                context.setSpecialPurposes( new ArrayList<String>( specialPurposes ) );

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("SetSpecialPurposes"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PrivateKeyMO.class );
            }
        });
    }

    @Override
    public byte[] generateCsr( final String identifier,
                               final String dn ) throws AccessorException {
        require( "identifier", identifier );

        return invoke(new AccessorMethod<byte[]>(){
            @Override
            public byte[] invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), (long) getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final PrivateKeyGenerateCsrContext context = new PrivateKeyGenerateCsrContext();
                context.setDn( dn );

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("GenerateCSR"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PrivateKeyGenerateCsrResult.class ).getCsrData();
            }
        });
    }

    @Override
    public PrivateKeyMO createKey( final PrivateKeyCreationContext context ) throws AccessorException {
        require( "context", context );
        require( "context.id", context.getId() );
        require( "context.dn", context.getDn() );

        return invoke(new AccessorMethod<PrivateKeyMO>(){
            @Override
            public PrivateKeyMO invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), (long) getTimeout(), Collections.singletonMap(ID_SELECTOR, context.getId()))[0];

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("CreateKey"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PrivateKeyMO.class );
            }
        });
    }

    @Override
    public byte[] exportKey( final String identifier,
                             final String alias,
                             final String password ) throws AccessorException {
        require( "identifier", identifier );
        require( "password", password );

        return invoke(new AccessorMethod<byte[]>(){
            @Override
            public byte[] invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), (long) getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final PrivateKeyExportContext context = new PrivateKeyExportContext();
                context.setAlias( alias );
                context.setPassword( password );

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("ExportKey"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PrivateKeyExportResult.class ).getPkcs12Data();
            }
        });
    }

    @Override
    public PrivateKeyMO importKey( final String identifier,
                                   final String alias,
                                   final String password,
                                   final byte[] keystoreBytes ) throws AccessorException {
        require( "identifier", identifier );
        require( "password", password );
        require( "keystoreBytes", keystoreBytes );

        return invoke(new AccessorMethod<PrivateKeyMO>(){
            @Override
            public PrivateKeyMO invoke() throws DatatypeConfigurationException, FaultException, JAXBException, SOAPException, IOException, AccessorException {
                final Resource resource =
                        getResourceFactory().find( getUrl(), getResourceUri(), (long) getTimeout(), Collections.singletonMap(ID_SELECTOR, identifier))[0];

                final PrivateKeyImportContext context = new PrivateKeyImportContext();
                context.setAlias( alias );
                context.setPassword( password );
                context.setPkcs12Data( keystoreBytes );

                final ResourceState resourceState =
                        resource.invoke( buildResourceScopedActionUri("ImportKey"), ManagedObjectFactory.write(context) );

                return ManagedObjectFactory.read( resourceState.getDocument(), PrivateKeyMO.class );
            }
        });
    }

    //- PACKAGE

    PrivateKeyMOAccessorImpl( final String url,
                              final String resourceUri,
                              final Class<PrivateKeyMO> typeClass,
                              final ResourceFactory resourceFactory,
                              final ResourceTracker resourceTracker ) {
        super( url, resourceUri, typeClass, resourceFactory, resourceTracker );
    }
}
