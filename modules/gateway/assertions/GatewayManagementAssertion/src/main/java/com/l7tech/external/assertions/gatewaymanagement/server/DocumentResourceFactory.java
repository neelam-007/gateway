package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import static com.l7tech.gateway.common.resources.ResourceType.*;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import com.l7tech.util.SyspropUtil;
import static com.l7tech.util.TextUtils.trim;
import org.springframework.transaction.PlatformTransactionManager;
import org.xml.sax.EntityResolver;

/**
 * The DocumentResourceFactory is a general purpose resource factory.
 *
 * <p>Currently the only resource type supported is XML Schemas, in the future
 * we may add support for other types (e.g. XSLT). When additional types are
 * supported this class will need to be refactored to allow "namespaced"
 * identifiers and could dispatch to different underlying resource factories
 * for the various resource types.</p>
 */                                                
@ResourceFactory.ResourceType(type=ResourceDocumentMO.class)
public class DocumentResourceFactory extends EntityManagerResourceFactory<ResourceDocumentMO, ResourceEntry, ResourceEntryHeader> {

    //- PUBLIC

    public DocumentResourceFactory( final RbacServices services,
                                    final SecurityFilter securityFilter,
                                    final PlatformTransactionManager transactionManager,
                                    final ResourceEntryManager resourceEntryManager,
                                    final EntityResolver entityResolver ) {
        super( false, false, services, securityFilter, transactionManager, resourceEntryManager );
        this.entityResolver = entityResolver;
    }

    //- PROTECTED

    @Override
    protected ResourceDocumentMO asResource( final ResourceEntry resourceEntry ) {
        final ResourceDocumentMO document = ManagedObjectFactory.createResourceDocument();

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setContent( resourceEntry.getContent() );
        resource.setSourceUrl( resourceEntry.getUri() );
        document.setProperties( getProperties( resourceEntry, ResourceEntry.class ) );

        switch ( resourceEntry.getType() ) {
            case XML_SCHEMA:
                resource.setType( ResourceHelper.SCHEMA_TYPE );
                if ( resourceEntry.getResourceKey1() != null ) {
                    document.getProperties().put( PROP_TARGET_NAMESPACE, resourceEntry.getResourceKey1() );
                }
                break;
            case DTD:
                resource.setType( ResourceHelper.DTD_TYPE );
                if ( resourceEntry.getResourceKey1() != null ) {
                    document.getProperties().put( PROP_PUBLIC_IDENTIFIER, resourceEntry.getResourceKey1() );
                }
                break;
        }

        document.setResource( resource );

        return document;
    }

    @Override
    protected ResourceEntry fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof ResourceDocumentMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected resource document");

        final ResourceDocumentMO resourceDocument = (ResourceDocumentMO) resource;
        final ResourceEntry resourceEntry = new ResourceEntry();

        final Resource resourceResource = resourceDocument.getResource();
        if ( resourceResource == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing resource");
        }

        resourceEntry.setUri( trim(resourceResource.getSourceUrl()) );
        resourceEntry.setContent( resourceResource.getContent() );

        if ( ResourceHelper.DTD_TYPE.equals( resourceResource.getType() ) ) {
            resourceEntry.setType( DTD );
            resourceEntry.setContentType( DTD.getMimeType() );
            final Option<String> publicIdentifier = getProperty( resourceDocument.getProperties(), PROP_PUBLIC_IDENTIFIER, Option.<String>none(), String.class ).map( trim() );
            if ( publicIdentifier.isSome() && !publicIdentifier.some().isEmpty() ) {
                resourceEntry.setResourceKey1( publicIdentifier.some() );
            } else {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES,
                        "property '"+PROP_PUBLIC_IDENTIFIER+"', must be a valid public identifier.");
            }
        } else if ( ResourceHelper.SCHEMA_TYPE.equals( resourceResource.getType() )) {
            resourceEntry.setType( XML_SCHEMA );
            resourceEntry.setContentType( XML_SCHEMA.getMimeType() );
            resourceEntry.setResourceKey1( getTns(resourceResource.getSourceUrl(), resourceResource.getContent()) );
        } else {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES,
                    "resource type '"+ResourceHelper.DTD_TYPE+"' or '"+ResourceHelper.SCHEMA_TYPE+"', expected");
        }

        setProperties( resourceEntry, resourceDocument.getProperties(), ResourceEntry.class );

        return resourceEntry;
    }

    @Override
    protected void updateEntity( final ResourceEntry oldEntity, final ResourceEntry newEntity ) throws InvalidResourceException {
        oldEntity.setUri( newEntity.getUri() );
        oldEntity.setType( newEntity.getType() );
        oldEntity.setContentType( newEntity.getContentType() );
        oldEntity.setContent( newEntity.getContent() );
        if ( newEntity.getDescription() != null ) {
            if ( newEntity.getDescription().isEmpty() ) {
                oldEntity.setDescription( null );
            } else {
                oldEntity.setDescription( newEntity.getDescription() );
            }
        }
        oldEntity.setResourceKey1( newEntity.getResourceKey1() );
        oldEntity.setResourceKey2( newEntity.getResourceKey2() );
        oldEntity.setResourceKey3( newEntity.getResourceKey3() );
    }

    //- PRIVATE

    private static final Boolean TNS_REQUIRED = SyspropUtil.getBoolean( "com.l7tech.external.assertions.gatewaymanagement.schemaTNSRequired", false );

    private static final String PROP_PUBLIC_IDENTIFIER = "publicIdentifier";
    private static final String PROP_TARGET_NAMESPACE = "targetNamespace";

    private final EntityResolver entityResolver;

    private String getTns( final String uri,
                           final String contents ) throws InvalidResourceException {
        String tns;
        try {
            tns = XmlUtil.getSchemaTNS(uri, contents, entityResolver);
        } catch (XmlUtil.BadSchemaException e) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid XML Schema '" + ExceptionUtils.getMessage(e) + "'");
        }

        if ( tns != null && tns.trim().isEmpty() ) {
            tns = null;
        }

        if ( tns == null && TNS_REQUIRED ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "XML Schema has no targetNamespace");
        }

        return tns;
    }

}

