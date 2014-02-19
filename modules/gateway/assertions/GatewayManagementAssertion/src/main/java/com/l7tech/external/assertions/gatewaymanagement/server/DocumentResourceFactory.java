package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import org.springframework.transaction.PlatformTransactionManager;
import org.xml.sax.EntityResolver;

import java.util.Map;

import static com.l7tech.gateway.common.resources.ResourceType.DTD;
import static com.l7tech.gateway.common.resources.ResourceType.XML_SCHEMA;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.extract;
import static com.l7tech.util.TextUtils.trim;

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
public class DocumentResourceFactory extends SecurityZoneableEntityManagerResourceFactory<ResourceDocumentMO, ResourceEntry, ResourceEntryHeader> {

    //- PUBLIC

    public DocumentResourceFactory( final RbacServices services,
                                    final SecurityFilter securityFilter,
                                    final PlatformTransactionManager transactionManager,
                                    final ResourceEntryManager resourceEntryManager,
                                    final EntityResolver entityResolver,
                                    final SecurityZoneManager securityZoneManager ) {
        super( false, true, services, securityFilter, transactionManager, resourceEntryManager, securityZoneManager );
        this.resourceEntryManager = resourceEntryManager;
        this.entityResolver = entityResolver;
    }

    //- PROTECTED

    @Override
    public ResourceDocumentMO asResource( final ResourceEntry resourceEntry ) {
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

        // handle SecurityZone
        doSecurityZoneAsResource( document, resourceEntry );

        return document;
    }

    @Override
    public ResourceEntry fromResource( final Object resource, boolean strict ) throws InvalidResourceException {
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

        // handle SecurityZone
        doSecurityZoneFromResource( resourceDocument, resourceEntry, strict );

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
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );
    }

    @Override
    public ResourceDocumentMO getResource(final Map<String, String> selectorMap) throws ResourceNotFoundException {
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,ResourceDocumentMO>>(){
            @Override
            public Either<ResourceNotFoundException,ResourceDocumentMO> execute() throws ObjectModelException {
                try {
                    ResourceEntry resource = selectResourceEntry(selectorMap);
                    EntityBag<ResourceEntry> entityBag = new EntityBag<ResourceEntry>(resource);
                    checkPermitted( OperationType.READ, null, entityBag.getEntity() );
                    return right( identify( asResource( entityBag ), entityBag.getEntity() ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    private ResourceEntry selectResourceEntry(Map<String, String> selectorMap) throws ResourceAccessException, ResourceNotFoundException {

        ResourceEntry resource = null;
        final String id = selectorMap.get( IDENTITY_SELECTOR );
        final String name = selectorMap.get( NAME_SELECTOR );

        if ( id == null && name == null ) {
            throw new InvalidResourceSelectors();
        }

        if ( id != null ) {
            try {
                resource = resourceEntryManager.findByPrimaryKey( toInternalId(id) );
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        if ( resource == null &&  name != null ) {
            EntityHeader header = new EntityHeader();
            header.setDescription(name);
            try {
                resource = resourceEntryManager.findResourceByUriAndType(name,null);
            } catch (FindException e) {
                handleObjectModelException(e);
            }
        }

        // Verify all selectors match (selectors must be AND'd)
        if ( resource != null ) {
            if ( id != null && !id.equalsIgnoreCase(resource.getId())) {
                resource = null;
            } else if ( name != null && !(name.equalsIgnoreCase(resource.getUri()))) {
                resource = null;
            }
        }

        if ( resource != null ) {
            resource = filterEntity( resource );
        }

        if ( resource == null ) {
            throw new ResourceNotFoundException("Resource not found " + selectorMap);
        } else {
            EntityContext.setEntityInfo( getType(), resource.getId() );
        }

        return resource;
    }
    //- PRIVATE

    private static final Boolean TNS_REQUIRED = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.gatewaymanagement.schemaTNSRequired", false );

    private static final String PROP_PUBLIC_IDENTIFIER = "publicIdentifier";
    private static final String PROP_TARGET_NAMESPACE = "targetNamespace";

    private final EntityResolver entityResolver;
    private final ResourceEntryManager resourceEntryManager;

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

