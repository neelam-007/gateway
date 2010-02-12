package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.communityschemas.SchemaEntryManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.transaction.PlatformTransactionManager;

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
public class DocumentResourceFactory extends EntityManagerResourceFactory<ResourceDocumentMO, SchemaEntry, EntityHeader> {

    //- PUBLIC

    public DocumentResourceFactory( final RbacServices services,
                                    final SecurityFilter securityFilter,
                                    final PlatformTransactionManager transactionManager,
                                    final SchemaEntryManager schemaEntryManager ) {
        super( true, false, services, securityFilter, transactionManager, schemaEntryManager );
    }

    //- PROTECTED

    @Override
    protected ResourceDocumentMO asResource( final SchemaEntry schemaEntry ) {
        final ResourceDocumentMO document = ManagedObjectFactory.createResourceDocument();

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setContent( schemaEntry.getSchema() );
        resource.setSourceUrl( schemaEntry.getName() );
        resource.setType( ResourceHelper.SCHEMA_TYPE );

        document.setResource( resource );

        return document;
    }

    @Override
    protected SchemaEntry fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof ResourceDocumentMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected resource document");

        final ResourceDocumentMO resourceDocument = (ResourceDocumentMO) resource;
        final SchemaEntry schemaEntry = new SchemaEntry();

        final Resource schemaResource = resourceDocument.getResource();
        if ( schemaResource == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing resource");
        } else if ( !ResourceHelper.SCHEMA_TYPE.equals( schemaResource.getType() ) ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "resource type '"+ResourceHelper.SCHEMA_TYPE+"', expected");
        }

        schemaEntry.setName( trim(schemaResource.getSourceUrl()) ); // don't use asName, since this is a URL, not really a name
        schemaEntry.setSchema( schemaResource.getContent() );
        schemaEntry.setSystem( false ); // Ignored for updates, false when creating new schemas
        schemaEntry.setTns( getTns(schemaResource.getContent()) );

        return schemaEntry;
    }

    @Override
    protected void updateEntity( final SchemaEntry oldEntity, final SchemaEntry newEntity ) throws InvalidResourceException {
        if ( oldEntity.isSystem() && !ALLOW_SYSTEM_UPDATES ) {
            throw new ResourceAccessException("Update of system XML Schema not permitted.");
        }

        oldEntity.setName( newEntity.getName() );
        oldEntity.setSchema( newEntity.getSchema() );
        oldEntity.setTns( newEntity.getTns() );        
    }

    //- PRIVATE

    private static final Boolean ALLOW_SYSTEM_UPDATES = SyspropUtil.getBoolean( "com.l7tech.externa.assertions.gatewaymanagement.schemaSystemUpdatable", false );
    private static final Boolean TNS_REQUIRED = SyspropUtil.getBoolean( "com.l7tech.externa.assertions.gatewaymanagement.schemaTNSRequired", true );

    private String getTns( final String contents ) throws InvalidResourceException {
        String tns;
        try {
            tns = XmlUtil.getSchemaTNS(contents);
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

